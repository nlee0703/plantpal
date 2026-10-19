package ca.gbc.comp3074.nicholas.plantpal.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import ca.gbc.comp3074.nicholas.plantpal.R
import java.io.File
import java.io.FileOutputStream

/** Copies picked photos into app-private storage, scaled down so lists stay fast (FR2). */
object PhotoStorage {
    private const val MAX_SIDE = 1080

    fun copyToPrivateStorage(context: Context, source: Uri, dir: File): String? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_SIDE || bounds.outHeight / (sample * 2) >= MAX_SIDE) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null

        val file = File(dir, "plant_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bitmap.recycle()
        file.absolutePath
    } catch (e: Exception) {
        null
    }

    /** Shows the photo at [path], or the leaf placeholder when there is none. */
    fun load(imageView: ImageView, path: String?) {
        val bitmap = path?.takeIf { File(it).exists() }?.let { decodeSampled(it, 600) }
        if (bitmap != null) {
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.setImageBitmap(bitmap)
        } else {
            imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            imageView.setImageResource(R.drawable.ic_plant_placeholder)
        }
    }

    private fun decodeSampled(path: String, target: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
