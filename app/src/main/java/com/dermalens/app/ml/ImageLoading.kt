package com.dermalens.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface

/**
 * Longest edge we'll decode a source photo down to. The model input is 640px square, and the
 * gallery crop math is all relative to the decoded bitmap's own dimensions, so nothing needs the
 * full 12MP -- and decoding it would cost ~48MB of heap per bitmap (twice over on the gallery
 * path, which decodes once to crop and once to infer). That's enough to OOM on a low-end device,
 * and an OutOfMemoryError is an Error rather than an Exception, so it sails straight past the
 * `catch (e: Exception)` handlers and takes the app down instead of falling back.
 */
private const val MAX_DECODE_EDGE = 2048

/**
 * Decodes [uri] into an upright bitmap, honouring the JPEG's EXIF orientation tag and
 * downsampling anything huge.
 *
 * Both matter, and neither is automatic. `BitmapFactory` ignores EXIF entirely, while Coil
 * (which draws the on-screen preview) applies it -- so before this existed, the pixels the user
 * saw and the pixels the model scored were rotated 90 degrees apart for essentially every camera
 * capture. CameraX writes its JPEGs in sensor orientation with a rotate tag rather than rotating
 * the pixels, so this isn't an edge case: it was every single photo taken in-app, fed to a model
 * trained exclusively on upright images.
 *
 * Returns null (rather than throwing) if the image can't be read at all.
 */
fun decodeUprightBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        // Pass 1: bounds only, to pick a power-of-two downsample factor.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            Log.e("DermaLens", "Could not read image bounds for $uri")
            return null
        }

        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_DECODE_EDGE) {
            sampleSize *= 2
        }

        // Pass 2: the real decode.
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = context.contentResolver.openInputStream(uri)
            ?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null

        val orientation = readExifOrientation(context, uri)
        applyOrientation(decoded, orientation)
    } catch (t: Throwable) {
        // Throwable, not Exception: OutOfMemoryError is the realistic failure here and callers
        // are all set up to fall back gracefully on null.
        Log.e("DermaLens", "Failed to decode image $uri", t)
        null
    }
}

private fun readExifOrientation(context: Context, uri: Uri): Int {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (t: Throwable) {
        // A missing or malformed EXIF block is normal (e.g. PNGs, some gallery providers) --
        // treat it as "already upright" rather than failing the whole decode.
        ExifInterface.ORIENTATION_NORMAL
    }
}

/** Rotates/flips [bitmap] into upright orientation. Returns [bitmap] itself when already upright. */
private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
        else -> return bitmap // ORIENTATION_NORMAL / UNDEFINED -- nothing to do
    }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (rotated != bitmap) bitmap.recycle()
    return rotated
}
