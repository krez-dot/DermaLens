package com.dermalens.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.dermalens.app.ui.screens.DetectionResult
import com.dermalens.app.ui.screens.mockDetectionResults
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

// TODO: rename to match the .tflite file you drop into app/src/main/assets/
private const val MODEL_FILE_NAME = "model.tflite"

// TODO: set to your model's actual input width/height (check with Netron or the export config)
private const val INPUT_SIZE = 224

// TODO: replace with your YOLOv11 training class order (index -> label), exactly as trained.
// Placeholder assumes the 6 Care Guide conditions; your model may end up covering more
// (HANDOFF.md notes a 9-class target once the multi-class merge lands).
private val CLASS_LABELS = listOf(
    "Acne Vulgaris", "Atopic Dermatitis", "Melasma", "Tinea", "Warts", "Scabies"
)

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
            val inputBuffer = preprocess(bitmap, INPUT_SIZE)
            val outputShape = interpreter.getOutputTensor(0).shape()
            val outputSize = outputShape.fold(1) { acc, d -> acc * d }
            val outputBuffer = ByteBuffer.allocateDirect(outputSize * 4).order(ByteOrder.nativeOrder())

            interpreter.run(inputBuffer, outputBuffer)

            outputBuffer.rewind()
            val values = FloatArray(outputSize)
            outputBuffer.asFloatBuffer().get(values)

            val (classIndex, confidence) = bestClass(values, outputShape)
            val label = CLASS_LABELS.getOrNull(classIndex) ?: return null
            val template = conditionTemplates[label] ?: return null
            template.copy(confidence = (confidence * 100f).coerceIn(0f, 100f))
        }
    } catch (e: Exception) {
        null
    }
}

private fun loadModelFile(context: Context): ByteBuffer? {
    return try {
        val afd = context.assets.openFd(MODEL_FILE_NAME)
        FileInputStream(afd.fileDescriptor).use { input ->
            input.channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
        }
    } catch (e: java.io.FileNotFoundException) {
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

private fun preprocess(bitmap: Bitmap, size: Int): ByteBuffer {
    val resized = Bitmap.createScaledBitmap(bitmap, size, size, true)
    val buffer = ByteBuffer.allocateDirect(4 * size * size * 3).order(ByteOrder.nativeOrder())
    val pixels = IntArray(size * size)
    resized.getPixels(pixels, 0, size, 0, 0, size, size)
    for (pixel in pixels) {
        buffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
        buffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
        buffer.putFloat((pixel and 0xFF) / 255f)
    }
    buffer.rewind()
    return buffer
}

/**
 * Picks the highest-confidence class from the model's raw output.
 *
 * TODO verify against your actual export shape (log interpreter.getOutputTensor(0).shape(),
 * or inspect the .tflite in Netron):
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
