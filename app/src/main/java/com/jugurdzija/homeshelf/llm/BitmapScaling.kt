package com.jugurdzija.homeshelf.llm

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import kotlin.math.max

fun Bitmap.downscaleForModel(maxDimension: Int, jpegQuality: Int): Bitmap {
    val scaleFactor = maxDimension.toFloat() / max(width, height)
    val scaled = if (scaleFactor < 1f) {
        this.scale(
            (width * scaleFactor).toInt().coerceAtLeast(1),
            (height * scaleFactor).toInt().coerceAtLeast(1)
        )
    } else {
        this
    }
    val output = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, jpegQuality, output)
    val bytes = output.toByteArray()
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
