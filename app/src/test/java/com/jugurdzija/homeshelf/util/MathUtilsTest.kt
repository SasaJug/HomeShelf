package com.jugurdzija.homeshelf.util

import com.jugurdzija.homeshelf.data.BoundingBox
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.MarkedItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun assertPixelsEqual(expected: List<Float>, actual: List<Float>, delta: Float = 0.01f) {
    assertEquals(expected.size, actual.size)
    expected.zip(actual).forEach { (e, a) -> assertEquals(e, a, delta) }
}

class MathUtilsTest {

    @Test
    fun `cosineSimilarity of identical vectors is 1`() {
        val a = floatArrayOf(1f, 2f, 3f)
        assertEquals(1f, cosineSimilarity(a, a), 0.0001f)
    }

    @Test
    fun `cosineSimilarity of orthogonal vectors is 0`() {
        val a = floatArrayOf(1f, 0f)
        val b = floatArrayOf(0f, 1f)
        assertEquals(0f, cosineSimilarity(a, b), 0.0001f)
    }

    @Test
    fun `cosineSimilarity handles zero vector without dividing by zero`() {
        val a = floatArrayOf(0f, 0f)
        val b = floatArrayOf(1f, 1f)
        assertEquals(0f, cosineSimilarity(a, b), 0.0001f)
    }

    @Test
    fun `mapLinesToImageCoords maps square canvas onto square bitmap 1 to 1`() {
        val lines = listOf(
            GuideLine(id = 1, isHorizontal = true, position = 0.5f),
            GuideLine(id = 2, isHorizontal = false, position = 0.25f)
        )

        val (hPixels, vPixels) = mapLinesToImageCoords(
            lines, canvasWidth = 100, canvasHeight = 100, bitmapWidth = 100, bitmapHeight = 100
        )

        assertPixelsEqual(listOf(50f), hPixels)
        assertPixelsEqual(listOf(25f), vPixels)
    }

    @Test
    fun `mapLinesToImageCoords centers bitmap when canvas aspect ratio is different`() {
        // Bitmap is square (100x100) but canvas is wider (200x100), so the bitmap is centered
        // horizontally with 50px of empty space on each side.
        val lines = listOf(
            GuideLine(id = 1, isHorizontal = false, position = 0.5f) // center of the canvas
        )

        val (_, vPixels) = mapLinesToImageCoords(
            lines, canvasWidth = 200, canvasHeight = 100, bitmapWidth = 100, bitmapHeight = 100
        )

        assertPixelsEqual(listOf(50f), vPixels)
    }

    @Test
    fun `mapLinesToImageCoords ignores lines outside of the bitmap edges`() {
        val lines = listOf(
            GuideLine(id = 1, isHorizontal = true, position = -1f),
            GuideLine(id = 2, isHorizontal = true, position = 2f)
        )

        val (hPixels, _) = mapLinesToImageCoords(
            lines, canvasWidth = 100, canvasHeight = 100, bitmapWidth = 100, bitmapHeight = 100
        )

        assertPixelsEqual(listOf(0f, 100f), hPixels)
    }

    @Test
    fun `mapLinesToImageCoords sorts results and separates by orientation`() {
        val lines = listOf(
            GuideLine(id = 1, isHorizontal = true, position = 0.8f),
            GuideLine(id = 2, isHorizontal = true, position = 0.2f),
            GuideLine(id = 3, isHorizontal = false, position = 0.6f),
            GuideLine(id = 4, isHorizontal = false, position = 0.1f)
        )

        val (hPixels, vPixels) = mapLinesToImageCoords(
            lines, canvasWidth = 100, canvasHeight = 100, bitmapWidth = 100, bitmapHeight = 100
        )

        assertPixelsEqual(listOf(20f, 80f), hPixels)
        assertPixelsEqual(listOf(10f, 60f), vPixels)
    }

    @Test
    fun `resolveCellName returns null when fewer than 2 lines exist per axis`() {
        val lines = listOf(GuideLine(id = 1, isHorizontal = true, position = 0.5f))

        val cellName = resolveCellName(
            lines, canvasWidth = 100, canvasHeight = 100, bitmapWidth = 100, bitmapHeight = 100,
            centerXFraction = 0.5f, centerYFraction = 0.5f
        )

        assertNull(cellName)
    }

    @Test
    fun `resolveCellName identifies the correct cell in a 2x2 grid`() {
        val lines = listOf(
            GuideLine(id = 1, isHorizontal = true, position = 0f),
            GuideLine(id = 2, isHorizontal = true, position = 0.5f),
            GuideLine(id = 3, isHorizontal = true, position = 1f),
            GuideLine(id = 4, isHorizontal = false, position = 0f),
            GuideLine(id = 5, isHorizontal = false, position = 0.5f),
            GuideLine(id = 6, isHorizontal = false, position = 1f)
        )

        val topLeft = resolveCellName(
            lines, canvasWidth = 100, canvasHeight = 100, bitmapWidth = 100, bitmapHeight = 100,
            centerXFraction = 0.25f, centerYFraction = 0.25f
        )
        val bottomRight = resolveCellName(
            lines, canvasWidth = 100, canvasHeight = 100, bitmapWidth = 100, bitmapHeight = 100,
            centerXFraction = 0.75f, centerYFraction = 0.75f
        )

        assertEquals("A1", topLeft)
        assertEquals("B2", bottomRight)
    }

    @Test
    fun `resolveCellName returns null when point falls outside every cell`() {
        val lines = listOf(
            GuideLine(id = 1, isHorizontal = true, position = 0.4f),
            GuideLine(id = 2, isHorizontal = true, position = 0.6f),
            GuideLine(id = 3, isHorizontal = false, position = 0.4f),
            GuideLine(id = 4, isHorizontal = false, position = 0.6f)
        )

        val cellName = resolveCellName(
            lines, canvasWidth = 100, canvasHeight = 100, bitmapWidth = 100, bitmapHeight = 100,
            centerXFraction = 0.9f, centerYFraction = 0.9f
        )

        assertNull(cellName)
    }

    @Test
    fun `mapLinesToImageCoords creates photo borders when no lines exist`() {
        val (hPixels, vPixels) = mapLinesToImageCoords(
            emptyList(), canvasWidth = 100, canvasHeight = 100, bitmapWidth = 200, bitmapHeight = 300
        )

        assertPixelsEqual(listOf(0f, 300f), hPixels)
        assertPixelsEqual(listOf(0f, 200f), vPixels)
    }

    @Test
    fun `resolveCellName resolves the whole photo to A1 when no lines exist`() {
        val cellName = resolveCellName(
            emptyList(), canvasWidth = 100, canvasHeight = 100, bitmapWidth = 100, bitmapHeight = 100,
            centerXFraction = 0.5f, centerYFraction = 0.5f
        )

        assertEquals("A1", cellName)
    }

    @Test
    fun `resolveItemsByCell groups items by their live-resolved cell`() {
        val lines = listOf(
            GuideLine(id = 1, isHorizontal = true, position = 0.5f),
            GuideLine(id = 2, isHorizontal = false, position = 0.5f)
        )
        val topLeftItem = MarkedItem(id = "1", name = "Rice", boundingBox = BoundingBox(0f, 0f, 0.1f, 0.1f))
        val bottomRightItem = MarkedItem(id = "2", name = "Sugar", boundingBox = BoundingBox(0.8f, 0.8f, 0.1f, 0.1f))

        val itemsByCell = resolveItemsByCell(
            listOf(topLeftItem, bottomRightItem), lines, bitmapWidth = 100, bitmapHeight = 100
        )

        assertEquals(listOf(topLeftItem), itemsByCell["A1"])
        assertEquals(listOf(bottomRightItem), itemsByCell["B2"])
    }

    @Test
    fun `resolveItemsByCell places every item in A1 when no grid was ever drawn`() {
        val item = MarkedItem(id = "1", name = "Rice", boundingBox = BoundingBox(0.8f, 0.8f, 0.1f, 0.1f))

        val itemsByCell = resolveItemsByCell(listOf(item), emptyList(), bitmapWidth = 100, bitmapHeight = 100)

        assertEquals(listOf(item), itemsByCell["A1"])
    }

    @Test
    fun `resolveItemsByCell uses whatever grid currently exists, regardless of when the item was marked`() {
        val item = MarkedItem(id = "1", name = "Rice", boundingBox = BoundingBox(0.8f, 0.8f, 0.1f, 0.1f))

        val beforeGrid = resolveItemsByCell(listOf(item), emptyList(), bitmapWidth = 100, bitmapHeight = 100)
        val afterGrid = resolveItemsByCell(
            listOf(item),
            listOf(
                GuideLine(id = 1, isHorizontal = true, position = 0.5f),
                GuideLine(id = 2, isHorizontal = false, position = 0.5f)
            ),
            bitmapWidth = 100, bitmapHeight = 100
        )

        assertEquals(listOf(item), beforeGrid["A1"])
        assertEquals(listOf(item), afterGrid["B2"])
        assertTrue(afterGrid["A1"].isNullOrEmpty())
    }

    @Test
    fun `resolveItemsByCell drops items that fall outside every cell`() {
        val lines = listOf(
            GuideLine(id = 1, isHorizontal = true, position = 0.4f),
            GuideLine(id = 2, isHorizontal = true, position = 0.6f),
            GuideLine(id = 3, isHorizontal = false, position = 0.4f),
            GuideLine(id = 4, isHorizontal = false, position = 0.6f)
        )
        val outsideItem = MarkedItem(id = "1", name = "Rice", boundingBox = BoundingBox(0.85f, 0.85f, 0.1f, 0.1f))

        val itemsByCell = resolveItemsByCell(listOf(outsideItem), lines, bitmapWidth = 100, bitmapHeight = 100)

        assertTrue(itemsByCell.isEmpty())
    }
}
