package com.dermalens.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.dermalens.app.ui.screens.DetectionResult
import com.dermalens.app.ui.screens.mockDetectionResults
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

private const val MODEL_FILE_NAME = "best.tflite"

// TEMPORARY: single-class test model (Melasma only), trained as an isolated sanity-check
// per HANDOFF.md's note on validating annotations before the full multi-class merge.
// Replace with the real class list (in training order) once the merged model is ready.
private val CLASS_LABELS = listOf("Melasma")

private val conditionTemplates: Map<String, DetectionResult> by lazy {
    mockDetectionResults.associateBy { it.condition }
}

/**
 * Runs on-device YOLOv11 inference on [imageUri]. Returns null if no model is bundled yet,
 * the image can't be read, or inference fails for any reason -- callers should fall back to
 * mockDetectionResults.random() in that case. Call this off the main thread.
 */
fun runYoloInference(context: Context, imageUri: String): DetectionResult? {
    return try {
        val modelBuffer = loadModelFile(context) ?: return null
        val bitmap = loadBitmap(context, imageUri) ?: return null

        Interpreter(modelBuffer).use { interpreter ->
            // Input size and layout are read from the model itself rather than assumed, since
            // exports vary between channels-last [1, H, W, 3] (the usual TFLite/mobile
            // convention) and channels-first [1, 3, H, W] (seen from some export paths, e.g.
            // this model's "serving_default_args_0" input -- a generic SavedModel signature
            // name that doesn't match Ultralytics' typical direct-export naming).
            val inputShape = interpreter.getInputTensor(0).shape()
            val inputDims = inputShape.drop(1) // drop batch dim
            val inputChannelsFirst = inputDims.getOrNull(0) == 3
            val inputHeight = if (inputChannelsFirst) inputDims.getOrElse(1) { 640 } else inputDims.getOrElse(0) { 640 }
            val inputWidth = if (inputChannelsFirst) inputDims.getOrElse(2) { 640 } else inputDims.getOrElse(1) { 640 }
            val outputShape = interpreter.getOutputTensor(0).shape()
            Log.d("DermaLens", "YOLO input=${inputShape.toList()} (channelsFirst=$inputChannelsFirst) output=${outputShape.toList()}")

            val inputBuffer = preprocess(bitmap, inputWidth, inputHeight, inputChannelsFirst)
            val outputSize = outputShape.fold(1) { acc, d -> acc * d }
            val outputBuffer = ByteBuffer.allocateDirect(outputSize * 4).order(ByteOrder.nativeOrder())

            interpreter.run(inputBuffer, outputBuffer)

            outputBuffer.rewind()
            val values = FloatArray(outputSize)
            outputBuffer.asFloatBuffer().get(values)

            val (classIndex, confidence) = bestClass(values, outputShape)
            Log.d("DermaLens", "YOLO result classIndex=$classIndex confidence=$confidence")
            val label = CLASS_LABELS.getOrNull(classIndex) ?: return null
            val template = conditionTemplates[label] ?: return null
            template.copy(confidence = (confidence * 100f).coerceIn(0f, 100f))
        }
    } catch (e: Exception) {
        Log.e("DermaLens", "YOLO inference failed", e)
        null
    }
}

private fun loadModelFile(context: Context): ByteBuffer? {
    Log.d("DermaLens", "YOLO looking for asset: $MODEL_FILE_NAME, assets list=${context.assets.list("")?.toList()}")
    return try {
        val afd = context.assets.openFd(MODEL_FILE_NAME)
        FileInputStream(afd.fileDescriptor).use { input ->
            input.channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
        }
    } catch (e: java.io.FileNotFoundException) {
        Log.e("DermaLens", "YOLO model asset not found: $MODEL_FILE_NAME", e)
        null // no model bundled yet -- caller falls back to mock results
    }
}

private fun loadBitmap(context: Context, imageUri: String): Bitmap? {
    return try {
        context.contentResolver.openInputStream(Uri.parse(imageUri))?.use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) {
        null
    }
}

private fun preprocess(bitmap: Bitmap, width: Int, height: Int, channelsFirst: Boolean): ByteBuffer {
    // Stretch-to-square, not letterboxed: tested both against this model and confidence was
    // roughly 5x higher with a stretch resize (~40% vs ~9%), which points to the training data
    // having been resized this same way -- likely Roboflow's "Resize: Stretch" preprocessing
    // preset (HANDOFF.md notes a Kaggle/Roboflow dataset). Revisit if the final merged model
    // turns out to use different preprocessing.
    val resized = Bitmap.createScaledBitmap(bitmap, width, height, true)
    val buffer = ByteBuffer.allocateDirect(4 * width * height * 3).order(ByteOrder.nativeOrder())
    val pixels = IntArray(width * height)
    resized.getPixels(pixels, 0, width, 0, 0, width, height)
    if (channelsFirst) {
        // NCHW: all R values, then all G values, then all B values (planar).
        for (channelShift in intArrayOf(16, 8, 0)) {
            for (pixel in pixels) {
                buffer.putFloat(((pixel shr channelShift) and 0xFF) / 255f)
            }
        }
    } else {
        // NHWC: R, G, B interleaved per pixel.
        for (pixel in pixels) {
            buffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
            buffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
            buffer.putFloat((pixel and 0xFF) / 255f)
        }
    }
    buffer.rewind()
    return buffer
}

/**
 * Picks the highest-confidence class from the model's raw output.
 *
 *  - Plain classifier [1, numClasses]: argmax directly (handled below).
 *  - YOLO detection head [1, 4+numClasses, numBoxes] or [1, numBoxes, 4+numClasses]:
 *    treated here as "does this class appear anywhere in the image" by taking the max
 *    score per class across all boxes -- fine for whole-image diagnosis since this screen
 *    doesn't render bounding boxes, but revisit if you later need localization.
 */
private fun bestClass(values: FloatArray, shape: IntArray): Pair<Int, Float> {
    if (shape.size == 2) {
        var bestIdx = 0
        var bestVal = values[0]
        for (i in values.indices) {
            if (values[i] > bestVal) { bestVal = values[i]; bestIdx = i }
        }
        return bestIdx to bestVal
    }

    val numClasses = CLASS_LABELS.size
    val dims = shape.drop(1) // drop batch dimension
    val channelsFirst = dims.getOrNull(0) == numClasses + 4
    val numChannels = if (channelsFirst) dims[0] else dims.getOrElse(1) { numClasses + 4 }
    val numBoxes = if (channelsFirst) dims.getOrElse(1) { 1 } else dims[0]

    val classScores = FloatArray(numClasses)
    for (box in 0 until numBoxes) {
        for (c in 0 until numClasses) {
            val channel = 4 + c
            val idx = if (channelsFirst) channel * numBoxes + box else box * numChannels + channel
            if (idx < values.size) {
                classScores[c] = maxOf(classScores[c], values[idx])
            }
        }
    }
    var bestIdx = 0
    var bestVal = classScores[0]
    for (i in classScores.indices) {
        if (classScores[i] > bestVal) { bestVal = classScores[i]; bestIdx = i }
    }
    return bestIdx to bestVal
}
