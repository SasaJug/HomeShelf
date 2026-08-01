package com.jugurdzija.homeshelf.llm

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

private val BoxColor = Color.rgb(0x29, 0xB6, 0xF6)

fun Bitmap.withMarkedBoxes(items: List<KnownItem>): Bitmap {
    val boxed = items.filter { it.box != null }
    if (boxed.isEmpty()) return this

    val annotated = copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(annotated)
    val shortSide = minOf(width, height).toFloat()
    val strokeWidth = (shortSide * 0.01f).coerceAtLeast(3f)
    val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        isAntiAlias = true
        color = BoxColor
    }

    boxed.forEach { item ->
        val box = item.box ?: return@forEach
        val left = box.x * width
        val top = box.y * height
        val right = left + box.width * width
        val bottom = top + box.height * height
        canvas.drawRect(left, top, right, bottom, boxPaint)
    }
    return annotated
}
