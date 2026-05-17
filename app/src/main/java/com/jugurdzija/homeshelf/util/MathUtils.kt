package com.jugurdzija.homeshelf.util

import com.jugurdzija.homeshelf.data.GuideLine
import kotlin.math.min
import kotlin.math.sqrt

// Calculates cosine similarity between two embeddings.
// ImageEmbedder does not expose this functionality directly.
// In order to enable saving embeddings instead of calculating them every time all over again, we need to use this.
fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
    var dot = 0f
    var normA = 0f
    var normB = 0f
    val len = min(a.size, b.size)
    for (i in 0 until len) {
        dot += a[i] * b[i]
        normA += a[i] * a[i]
        normB += b[i] * b[i]
    }
    return if (normA == 0f || normB == 0f) 0f else dot / (sqrt(normA) * sqrt(normB))
}


fun mapLinesToImageCoords(
    lines: List<GuideLine>,
    canvasWidth: Int,
    canvasHeight: Int,
    bitmapWidth: Int,
    bitmapHeight: Int
): Pair<List<Float>, List<Float>> {
    val scale = min(canvasWidth.toFloat() / bitmapWidth, canvasHeight.toFloat() / bitmapHeight)
    val renderedW = bitmapWidth * scale
    val renderedH = bitmapHeight * scale
    val offsetX = (canvasWidth - renderedW) / 2f
    val offsetY = (canvasHeight - renderedH) / 2f

    val hPixels = lines
        .filter { it.isHorizontal }
        .map { line ->
            val canvasY = line.position * canvasHeight
            ((canvasY - offsetY) / renderedH * bitmapHeight).coerceIn(0f, bitmapHeight.toFloat())
        }
        .sorted()

    val vPixels = lines
        .filter { !it.isHorizontal }
        .map { line ->
            val canvasX = line.position * canvasWidth
            ((canvasX - offsetX) / renderedW * bitmapWidth).coerceIn(0f, bitmapWidth.toFloat())
        }
        .sorted()

    return hPixels to vPixels
}
