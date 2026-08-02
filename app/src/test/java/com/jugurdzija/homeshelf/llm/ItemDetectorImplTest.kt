package com.jugurdzija.homeshelf.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ItemDetectorImplTest {

    @Test
    fun `maps ymin xmin ymax xmax box into relative x y width height`() {
        val json = DetectedItemJson(
            name = "Rice",
            box = listOf(100, 200, 300, 500),
            isTransparentContainer = false
        )

        val item = json.toDetectedItem()

        assertEquals("Rice", item?.name)
        assertEquals(0.2f, item?.box?.x)
        assertEquals(0.1f, item?.box?.y)
        assertEquals(0.3f, item?.box?.width)
        assertEquals(0.2f, item?.box?.height)
        assertEquals(false, item?.isTransparentContainer)
    }

    @Test
    fun `preserves isTransparentContainer flag`() {
        val json = DetectedItemJson(
            name = "Olive oil",
            box = listOf(0, 0, 1000, 1000),
            isTransparentContainer = true
        )

        val item = json.toDetectedItem()

        assertEquals(true, item?.isTransparentContainer)
    }

    @Test
    fun `returns null when box does not have exactly 4 coordinates`() {
        val tooFew = DetectedItemJson(name = "Rice", box = listOf(1, 2, 3), isTransparentContainer = false)
        val tooMany = DetectedItemJson(name = "Rice", box = listOf(1, 2, 3, 4, 5), isTransparentContainer = false)
        val empty = DetectedItemJson(name = "Rice", box = emptyList(), isTransparentContainer = false)

        assertNull(tooFew.toDetectedItem())
        assertNull(tooMany.toDetectedItem())
        assertNull(empty.toDetectedItem())
    }
}
