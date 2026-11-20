package com.lukuman.freechat.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ImageCompressor {
    private const val MAX_DIMENSION_PX = 800
    private const val MAX_JPEG_BYTES = 150 * 1024 // 150 KB

    suspend fun compressUriToBase64(context: Context, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver

            val orientation = resolver.openInputStream(uri)?.use { exifStream ->
                runCatching {
                    ExifInterface(exifStream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL

            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                return@withContext null
            }

            var inSampleSize = 1
            val rawMax = max(boundsOptions.outWidth, boundsOptions.outHeight)
            while (rawMax / (inSampleSize * 2) >= MAX_DIMENSION_PX) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val decoded = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            val rotated = applyExifOrientation(decoded, orientation)
            if (rotated !== decoded) {
                decoded.recycle()
            }

            var currentBitmap = resizeToMaxDimension(rotated, MAX_DIMENSION_PX)
            if (currentBitmap !== rotated) {
                rotated.recycle()
            }

            var compressedBytes: ByteArray
            var scaleStep = 0
            while (true) {
                compressedBytes = compressUnderLimit(currentBitmap)
                if (compressedBytes.size < MAX_JPEG_BYTES || scaleStep >= 3) {
                    break
                }
                scaleStep++
                val nextWidth = (currentBitmap.width * 0.8f).roundToInt().coerceAtLeast(160)
                val nextHeight = (currentBitmap.height * 0.8f).roundToInt().coerceAtLeast(160)
                val scaled = Bitmap.createScaledBitmap(currentBitmap, nextWidth, nextHeight, true)
                if (scaled !== currentBitmap) {
                    currentBitmap.recycle()
                }
                currentBitmap = scaled
            }

            currentBitmap.recycle()

            if (compressedBytes.size >= MAX_JPEG_BYTES) {
                return@withContext null
            }

            Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
        }

    fun decodeBase64Bitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return runCatching {
            val clean = base64.substringAfter("base64,", base64)
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    private fun resizeToMaxDimension(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val longest = max(width, height)
        if (longest <= maxDim) return bitmap

        val ratio = maxDim.toFloat() / longest.toFloat()
        val targetWidth = (width * ratio).roundToInt().coerceAtLeast(1)
        val targetHeight = (height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun compressUnderLimit(bitmap: Bitmap): ByteArray {
        var quality = 85
        val stream = ByteArrayOutputStream()
        while (quality >= 25) {
            stream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            if (stream.size() < MAX_JPEG_BYTES) {
                return stream.toByteArray()
            }
            quality -= if (quality > 45) 10 else 5
        }
        return stream.toByteArray()
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
