package com.jugurdzija.homeshelf.util

import com.jugurdzija.homeshelf.data.GuideLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
