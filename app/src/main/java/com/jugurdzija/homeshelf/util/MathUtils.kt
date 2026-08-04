package com.jugurdzija.homeshelf.util

import android.graphics.RectF
import com.jugurdzija.homeshelf.data.BoundingBox
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.MarkedItem
import com.jugurdzija.homeshelf.llm.GeneratedGuideLine
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
    if (lines.isEmpty()) {
        return listOf(0f, bitmapHeight.toFloat()) to listOf(0f, bitmapWidth.toFloat())
    }
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

fun mapGeneratedLinesToCanvasGuideLines(
    generated: List<GeneratedGuideLine>,
    canvasWidth: Int,
    canvasHeight: Int,
    bitmapWidth: Int,
    bitmapHeight: Int,
    startId: Int
): List<GuideLine> {
    val scale = min(canvasWidth.toFloat() / bitmapWidth, canvasHeight.toFloat() / bitmapHeight)
    val renderedW = bitmapWidth * scale
    val renderedH = bitmapHeight * scale
    val offsetX = (canvasWidth - renderedW) / 2f
    val offsetY = (canvasHeight - renderedH) / 2f

    return generated.mapIndexed { index, line ->
        val canvasPosition = if (line.isHorizontal) {
            (offsetY + line.position * renderedH) / canvasHeight
        } else {
            (offsetX + line.position * renderedW) / canvasWidth
        }
        GuideLine(
            id = startId + index,
            isHorizontal = line.isHorizontal,
            position = canvasPosition.coerceIn(0f, 1f)
        )
    }
}

// Finds out which grid cell a point falls into.
fun resolveCellName(
    guideLines: List<GuideLine>,
    canvasWidth: Int,
    canvasHeight: Int,
    bitmapWidth: Int,
    bitmapHeight: Int,
    centerXFraction: Float,
    centerYFraction: Float
): String? {
    val (hPixels, vPixels) = mapLinesToImageCoords(guideLines, canvasWidth, canvasHeight, bitmapWidth, bitmapHeight)
    if (hPixels.size < 2 || vPixels.size < 2) return null
    val xPx = centerXFraction * bitmapWidth
    val yPx = centerYFraction * bitmapHeight
    val rowIdx = hPixels.zipWithNext().indexOfFirst { (top, bottom) -> yPx in top..bottom }
    val colIdx = vPixels.zipWithNext().indexOfFirst { (left, right) -> xPx in left..right }
    return if (rowIdx < 0 || colIdx < 0) null else "${'A' + colIdx}${rowIdx + 1}"
}

// Groups marked items by the grid cell their center currently falls into.
fun resolveItemsByCell(
    items: List<MarkedItem>,
    guideLines: List<GuideLine>,
    bitmapWidth: Int,
    bitmapHeight: Int
): Map<String, List<MarkedItem>> {
    return items.mapNotNull { item ->
        val cellName = resolveCellName(
            guideLines, bitmapWidth, bitmapHeight, bitmapWidth, bitmapHeight,
            item.boundingBox.x + item.boundingBox.width / 2f,
            item.boundingBox.y + item.boundingBox.height / 2f
        )
        cellName?.let { it to item }
    }.groupBy({ it.first }, { it.second })
}

// Calculates the rectangle of a grid cell using the provided guidelines and dimensions.
fun cellBoundsAsFraction(
    guideLines: List<GuideLine>,
    canvasWidth: Int,
    canvasHeight: Int,
    bitmapWidth: Int,
    bitmapHeight: Int,
    cellName: String
): RectF? {
    val colIdx = cellName.firstOrNull()?.minus('A') ?: return null
    val rowIdx = cellName.drop(1).toIntOrNull()?.minus(1) ?: return null
    if (colIdx < 0 || rowIdx < 0) return null
    val (hPixels, vPixels) = mapLinesToImageCoords(guideLines, canvasWidth, canvasHeight, bitmapWidth, bitmapHeight)
    if (rowIdx + 1 >= hPixels.size || colIdx + 1 >= vPixels.size) return null
    val top = hPixels[rowIdx]
    val bottom = hPixels[rowIdx + 1]
    val left = vPixels[colIdx]
    val right = vPixels[colIdx + 1]
    return RectF(left / bitmapWidth, top / bitmapHeight, right / bitmapWidth, bottom / bitmapHeight)
}

// Converts an item's bounding box from full-photo coordinates into coordinates relative to its cell.
// It is used to position item's box accurately within a cropped cell image.
fun toCellLocalFraction(itemBox: BoundingBox, cellBounds: RectF): BoundingBox {
    val cellWidth = cellBounds.width()
    val cellHeight = cellBounds.height()
    if (cellWidth <= 0f || cellHeight <= 0f) return itemBox
    return BoundingBox(
        x = (itemBox.x - cellBounds.left) / cellWidth,
        y = (itemBox.y - cellBounds.top) / cellHeight,
        width = itemBox.width / cellWidth,
        height = itemBox.height / cellHeight
    )
}

// Converts an item's bounding box from cell-local coordinates back into full-photo coordinates.
// It aligns newly detected cell items with the standard coordinate system used by full-photo marked items.
fun fromCellLocalFraction(cellLocalBox: BoundingBox, cellBounds: RectF): BoundingBox {
    val cellWidth = cellBounds.width()
    val cellHeight = cellBounds.height()
    return BoundingBox(
        x = cellBounds.left + cellLocalBox.x * cellWidth,
        y = cellBounds.top + cellLocalBox.y * cellHeight,
        width = cellLocalBox.width * cellWidth,
        height = cellLocalBox.height * cellHeight
    )
}
