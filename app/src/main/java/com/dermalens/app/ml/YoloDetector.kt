package com.dermalens.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.dermalens.app.ui.screens.DetectionResult
import com.dermalens.app.ui.screens.DifferentialCandidate
import com.dermalens.app.ui.screens.NormalizedBox
import com.dermalens.app.ui.screens.mockDetectionResults
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

private const val MODEL_FILE_NAME = "best.tflite"

// RESET (fresh training start, 2026-09-11): no model is currently bundled -- app/src/main/assets/
// best.tflite was removed along with all local training run data, to redo data verification and
// training from scratch. loadModelFile() returns null when the asset is missing, and
// runYoloInference() returns null immediately after that (before CLASS_LABELS is ever read), so an
// empty list here is safe -- but fill this back in with the real class order the next model was
// trained against (matching the training notebook's CONDITIONS list exactly) once one exists,
// or every result will be silently mislabeled.
private val CLASS_LABELS = emptyList<String>()

private val conditionTemplates: Map<String, DetectionResult> by lazy {
    mockDetectionResults.associateBy { it.condition }
}

/**
 * The one confidence floor for this model, taken from its own F1-Confidence curve.
 *
 * RESET (fresh training start, 2026-09-11): no model is bundled right now, so this placeholder
 * value means nothing yet -- do not carry over a number from a deleted model. Once a new model is
 * trained, read its own BoxF1_curve.png ("all classes X at Y") and set this to Y. Every model needs
 * its own check here; the right floor is not a fixed constant across different models or datasets.
 *
 * This deliberately drives BOTH the per-box candidate filter and the "is the verdict good enough
 * to show" gate, on purpose -- keeping those as two separate constants previously let a scan clear
 * the verdict gate while every one of its boxes got filtered out separately, so the app would name
 * a condition and draw nothing. Keep them unified when this gets filled back in.
 */
private const val CONFIDENCE_THRESHOLD = 0.25f // placeholder only -- re-derive from the next model's own F1 curve

private const val MIN_CONFIDENCE_PERCENT = CONFIDENCE_THRESHOLD * 100f

// Below this, a class's score is treated as noise rather than a plausible differential -- with
// only CONFIDENCE_THRESHOLD needed to commit to the primary result, a much lower floor here
// still filters out the long tail of near-zero scores every untrained class gets.
private const val MIN_DIFFERENTIAL_PERCENT = 15f
private const val MAX_DIFFERENTIALS = 2

private fun lowConfidenceResult(confidencePercent: Float) = DetectionResult(
    condition = "No Clear Condition Detected",
    confidence = confidencePercent,
    severity = "Unclear",
    description = "The scan didn't clearly match any condition this app currently recognizes. This can happen if the photo isn't of skin, is blurry or poorly lit, or doesn't clearly show an affected area.",
    symptoms = listOf(
        "Retake the photo in good, even lighting",
        "Make sure the affected skin fills the guide frame",
        "Hold the camera steady and in focus"
    ),
    recommendation = "If you have visible skin concerns, consult a licensed dermatologist for an accurate diagnosis.",
    color = Color(0xFF6B7280),
    isLowConfidence = true
)

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

            val (classIndex, confidence, boxes, classScores) =
                bestClass(values, outputShape, inputWidth, inputHeight) ?: return null
            val confidencePercent = (confidence * 100f).coerceIn(0f, 100f)
            Log.d("DermaLens", "YOLO result classIndex=$classIndex confidence=$confidence boxes=$boxes")
            if (confidencePercent < MIN_CONFIDENCE_PERCENT) {
                return lowConfidenceResult(confidencePercent)
            }
            val label = CLASS_LABELS.getOrNull(classIndex) ?: return null
            val template = conditionTemplates[label] ?: return null

            val differentials = classScores.indices
                .filter { it != classIndex }
                .map { idx -> idx to (classScores[idx] * 100f).coerceIn(0f, 100f) }
                .filter { (_, percent) -> percent >= MIN_DIFFERENTIAL_PERCENT }
                .sortedByDescending { (_, percent) -> percent }
                .take(MAX_DIFFERENTIALS)
                .mapNotNull { (idx, percent) ->
                    val candidateLabel = CLASS_LABELS.getOrNull(idx) ?: return@mapNotNull null
                    val candidateTemplate = conditionTemplates[candidateLabel] ?: return@mapNotNull null
                    DifferentialCandidate(candidateLabel, percent, candidateTemplate.distinguishingFeature, candidateTemplate.color)
                }

            template.copy(confidence = confidencePercent, boundingBoxes = boxes, differentials = differentials)
        }
    } catch (t: Throwable) {
        // Throwable rather than Exception: OutOfMemoryError from a large decode or a big tensor
        // allocation is an Error, and would otherwise sail past this handler and crash the app.
        Log.e("DermaLens", "YOLO inference failed", t)
        null
    }
}

/**
 * Shown when inference couldn't run at all -- no bundled model, an unreadable photo, a
 * model/label mismatch, a TFLite failure.
 *
 * This exists because the call site used to fall back to `mockDetectionResults.random()`, which
 * meant any of those failures presented the user with a randomly chosen skin condition and a
 * plausible-looking confidence number. For a diagnostic app that's the worst available failure
 * mode -- an honest error is the only acceptable one.
 */
fun analysisFailedResult() = DetectionResult(
    condition = "Analysis Unavailable",
    confidence = 0f,
    severity = "Unknown",
    description = "The scan couldn't be analyzed on this device. This is a problem with the app rather than with your photo, so retaking it probably won't help.",
    symptoms = listOf(
        "Try closing and reopening the app",
        "If it keeps happening, report it with the date and time of the scan"
    ),
    recommendation = "No result was produced, so nothing here should be read as a diagnosis. For any skin concern you're worried about, consult a licensed dermatologist.",
    color = Color(0xFF6B7280),
    isLowConfidence = true
)

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

// EXIF-aware and downsampled -- a raw BitmapFactory.decodeStream here fed the model sideways
// photos, since CameraX saves JPEGs in sensor orientation with a rotate tag. See ImageLoading.kt.
private fun loadBitmap(context: Context, imageUri: String): Bitmap? =
    decodeUprightBitmap(context, Uri.parse(imageUri))

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

private const val NMS_IOU_THRESHOLD = 0.45f // Ultralytics' standard NMS overlap cutoff

/** [classScores] holds every class's own confidence (not just the winner's), so callers can
 *  rank differentials from the model's actual output instead of only seeing the top pick. */
private data class ClassificationResult(
    val bestIndex: Int,
    val bestConfidence: Float,
    val boxes: List<NormalizedBox>,
    val classScores: FloatArray
)

/**
 * Picks the highest-confidence class from the model's raw output, along with every distinct
 * region of that class found in the image (normalized to [0,1] relative to the model's square
 * input), for drawing on the result screen. A condition like melasma is often bilateral, so
 * collapsing to a single "best" box would silently hide a second affected region.
 *
 *  - Plain classifier [1, numClasses]: argmax directly, no boxes (nothing to localize).
 *  - YOLO detection head [1, 4+numClasses, numBoxes] or [1, numBoxes, 4+numClasses]: every
 *    candidate box scoring above [BOX_CONFIDENCE_THRESHOLD] for the winning class is kept, then
 *    non-max suppression collapses duplicate/overlapping detections of the same region while
 *    keeping genuinely separate ones (e.g. left cheek and right cheek).
 */
private fun bestClass(values: FloatArray, shape: IntArray, inputWidth: Int, inputHeight: Int): ClassificationResult? {
    if (shape.size == 2) {
        var bestIdx = 0
        var bestVal = values[0]
        for (i in values.indices) {
            if (values[i] > bestVal) { bestVal = values[i]; bestIdx = i }
        }
        return ClassificationResult(bestIdx, bestVal, emptyList(), values.copyOf())
    }

    val dims = shape.drop(1) // drop batch dimension

    // Which dim is which is decided by SIZE, not by matching against CLASS_LABELS.size. A YOLO
    // head emits 4+numClasses channels (5 for one class, 10 for six) against thousands of
    // candidate boxes (8400 at 640px), so the smaller dim is always the channel dim. Keying off
    // CLASS_LABELS.size instead -- as this did -- meant that swapping best.tflite without also
    // editing CLASS_LABELS flipped the layout guess, and the tensor got read transposed: garbage
    // confidences, no exception, nothing in the log. Given how often the model is being swapped
    // right now, that's a landmine worth removing.
    val channelsFirst = dims[0] <= dims.getOrElse(1) { dims[0] }
    val numChannels = if (channelsFirst) dims[0] else dims.getOrElse(1) { dims[0] }
    val numBoxes = if (channelsFirst) dims.getOrElse(1) { 1 } else dims[0]
    val numClasses = numChannels - 4

    // Fail loudly instead of scoring nonsense: if the bundled model's class count doesn't match
    // CLASS_LABELS, every label lookup below would be off by however far they disagree.
    if (numClasses != CLASS_LABELS.size) {
        Log.e(
            "DermaLens",
            "Model/label mismatch: $MODEL_FILE_NAME outputs $numClasses classes but CLASS_LABELS " +
                "has ${CLASS_LABELS.size} (${CLASS_LABELS.joinToString()}). Refusing to guess."
        )
        return null
    }

    fun valueAt(channel: Int, box: Int): Float {
        val idx = if (channelsFirst) channel * numBoxes + box else box * numChannels + channel
        return if (idx < values.size) values[idx] else 0f
    }

    val classScores = FloatArray(numClasses)
    for (box in 0 until numBoxes) {
        for (c in 0 until numClasses) {
            val score = valueAt(4 + c, box)
            if (score > classScores[c]) classScores[c] = score
        }
    }
    var bestIdx = 0
    var bestVal = classScores[0]
    for (i in classScores.indices) {
        if (classScores[i] > bestVal) { bestVal = classScores[i]; bestIdx = i }
    }

    // Ultralytics TFLite detection heads emit box center/size either already normalized to
    // [0,1], or in pixel units relative to the input size, depending on export settings -- a
    // normalized coordinate can never exceed 1, so use that to tell which convention this
    // export uses instead of assuming.
    fun toNormalizedBox(box: Int): NormalizedBox {
        val rawCx = valueAt(0, box)
        val rawCy = valueAt(1, box)
        val rawW = valueAt(2, box)
        val rawH = valueAt(3, box)
        val looksNormalized = rawCx <= 1.5f && rawCy <= 1.5f && rawW <= 1.5f && rawH <= 1.5f
        val cx = if (looksNormalized) rawCx else rawCx / inputWidth
        val cy = if (looksNormalized) rawCy else rawCy / inputHeight
        val w = if (looksNormalized) rawW else rawW / inputWidth
        val h = if (looksNormalized) rawH else rawH / inputHeight
        return NormalizedBox(
            left = (cx - w / 2f).coerceIn(0f, 1f),
            top = (cy - h / 2f).coerceIn(0f, 1f),
            right = (cx + w / 2f).coerceIn(0f, 1f),
            bottom = (cy + h / 2f).coerceIn(0f, 1f)
        )
    }

    val candidates = (0 until numBoxes)
        .map { box -> box to valueAt(4 + bestIdx, box) }
        .filter { (_, score) -> score >= CONFIDENCE_THRESHOLD }
        .sortedByDescending { (_, score) -> score }
        .map { (box, score) -> toNormalizedBox(box) to score }

    val kept = mutableListOf<NormalizedBox>()
    for ((candidateBox, _) in candidates) {
        if (kept.none { iou(it, candidateBox) > NMS_IOU_THRESHOLD }) {
            kept.add(candidateBox)
        }
    }

    return ClassificationResult(bestIdx, bestVal, kept, classScores)
}

private fun iou(a: NormalizedBox, b: NormalizedBox): Float {
    val left = maxOf(a.left, b.left)
    val top = maxOf(a.top, b.top)
    val right = minOf(a.right, b.right)
    val bottom = minOf(a.bottom, b.bottom)
    val intersection = (right - left).coerceAtLeast(0f) * (bottom - top).coerceAtLeast(0f)
    val areaA = (a.right - a.left) * (a.bottom - a.top)
    val areaB = (b.right - b.left) * (b.bottom - b.top)
    val union = areaA + areaB - intersection
    return if (union <= 0f) 0f else intersection / union
}
