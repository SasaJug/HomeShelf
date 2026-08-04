package com.jugurdzija.homeshelf.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageCompletenessTest {

    @Test
    fun `calculateCompleteness is COMPLETE when both grid and items exist`() {
        val data = ReferencePhotoData(
            guideLines = listOf(GuideLine(id = 1, isHorizontal = true, position = 0.5f)),
            markedItems = listOf(MarkedItem(id = "1", name = "Rice", boundingBox = BoundingBox(0f, 0f, 0.1f, 0.1f)))
        )

        assertEquals(StorageCompleteness.COMPLETE, calculateCompleteness(data))
    }

    @Test
    fun `calculateCompleteness is NO_GRID when items exist but no guide lines`() {
        val data = ReferencePhotoData(
            guideLines = emptyList(),
            markedItems = listOf(MarkedItem(id = "1", name = "Rice", boundingBox = BoundingBox(0f, 0f, 0.1f, 0.1f)))
        )

        assertEquals(StorageCompleteness.NO_GRID, calculateCompleteness(data))
    }

    @Test
    fun `calculateCompleteness is NO_ITEMS when no items exist regardless of grid`() {
        val data = ReferencePhotoData(
            guideLines = listOf(GuideLine(id = 1, isHorizontal = true, position = 0.5f)),
            markedItems = emptyList()
        )

        assertEquals(StorageCompleteness.NO_ITEMS, calculateCompleteness(data))
    }

    @Test
    fun `calculateCompleteness prefers NO_ITEMS when both grid and items are missing`() {
        val data = ReferencePhotoData(guideLines = emptyList(), markedItems = emptyList())

        assertEquals(StorageCompleteness.NO_ITEMS, calculateCompleteness(data))
    }
}
