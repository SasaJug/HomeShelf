package com.jugurdzija.homeshelf.llm

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

private val BoxColors = listOf(
    Color.rgb(0xE5, 0x39, 0x35), // red
    Color.rgb(0x1E, 0x88, 0xE5), // blue
    Color.rgb(0x43, 0xA0, 0x47), // green
    Color.rgb(0xFB, 0x8C, 0x00) // orange
)

fun Bitmap.withLabeledBoxes(items: List<KnownItem>): Bitmap {
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
    }
    val textFillPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        textSize = (shortSide * 0.05f).coerceAtLeast(24f)
        isAntiAlias = true
    }
    val textOutlinePaint = Paint().apply {
        style = Paint.Style.STROKE
        this.strokeWidth = strokeWidth * 0.6f
        textSize = textFillPaint.textSize
        isAntiAlias = true
    }

    boxed.forEachIndexed { index, item ->
        val box = item.box ?: return@forEachIndexed
        val color = BoxColors[index % BoxColors.size]
        boxPaint.color = color
        textOutlinePaint.color = color

        val left = box.x * width
        val top = box.y * height
        val right = left + box.width * width
        val bottom = top + box.height * height
        canvas.drawRect(left, top, right, bottom, boxPaint)

        val padding = strokeWidth * 2
        val textHeight = textFillPaint.textSize
        val baselineY = when {
            top - textHeight - padding >= 0f -> top - padding
            bottom + textHeight + padding <= height -> bottom + textHeight + padding
            else -> top + textHeight + padding
        }
        canvas.drawText(item.id, left + padding, baselineY, textOutlinePaint)
        canvas.drawText(item.id, left + padding, baselineY, textFillPaint)
    }
    return annotated
}
