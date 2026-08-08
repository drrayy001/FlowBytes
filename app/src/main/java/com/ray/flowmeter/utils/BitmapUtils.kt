package com.ray.flowmeter.utils

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import java.io.InputStream

/**
 * Helper utility for decoding and downsampling Bitmaps using BitmapFactory.Options
 * to reduce memory footprint and prevent OutOfMemoryErrors as recommended by Google Play Console.
 */
object BitmapUtils {

    /**
     * Calculates the sample size value as a power of two based on target width and height.
     */
    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Decodes a bitmap resource with downsampling using BitmapFactory.Options.
     */
    fun decodeSampledBitmapFromResource(
        res: Resources,
        resId: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap {
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeResource(res, resId, this)

            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
            inJustDecodeBounds = false
            BitmapFactory.decodeResource(res, resId, this)
        }
    }

    /**
     * Decodes a bitmap from a file path with downsampling.
     */
    fun decodeSampledBitmapFromFile(
        pathName: String,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(pathName, this)

            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
            inJustDecodeBounds = false
            BitmapFactory.decodeFile(pathName, this)
        }
    }

    /**
     * Decodes a bitmap from a byte array with downsampling.
     */
    fun decodeSampledBitmapFromByteArray(
        data: ByteArray,
        offset: Int = 0,
        length: Int = data.size,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(data, offset, length, this)

            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
            inJustDecodeBounds = false
            BitmapFactory.decodeByteArray(data, offset, length, this)
        }
    }

    /**
     * Decodes a bitmap from an InputStream supplier with downsampling.
     * Supplier is needed because the stream must be re-opened for the second decoding pass.
     */
    fun decodeSampledBitmapFromStream(
        inputStreamSupplier: () -> InputStream?,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        inputStreamSupplier()?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: return null

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        return inputStreamSupplier()?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }
}

/**
 * Extension function to safely convert a Drawable to a downsampled Bitmap
 * of target dimensions (defaulting to 96x96 px for app icons).
 */
fun Drawable.toDownsampledBitmap(width: Int = 96, height: Int = 96): Bitmap {
    return toBitmap(width = width, height = height)
}
