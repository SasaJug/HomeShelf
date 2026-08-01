package com.jugurdzija.homeshelf.llm

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.data.BoundingBox
import kotlinx.serialization.Serializable

data class KnownItem(
    val id: String,
    val name: String,
    val isTransparentContainer: Boolean,
    val box: BoundingBox? = null
)

data class CellPair(
    val cellId: String,
    val referenceBitmap: Bitmap,
    val newBitmap: Bitmap,
    val knownItems: List<KnownItem> = emptyList()
)

@Serializable
enum class ItemChange {
    UNCHANGED,
    REMOVED,
    REPLACED,
    ADDED,
    PARTIALLY_CONSUMED,
    FULLY_CONSUMED,
    PARTIALLY_FILLED,
    FULLY_FILLED,
    UNKNOWN
}

@Serializable
data class ItemDiffResult(
    val id: String,
    val change: ItemChange,
    val description: String,
    val name: String? = null,
    val box: BoundingBox? = null,
    val isTransparentContainer: Boolean? = null
)

@Serializable
data class CellDiffResult(
    val cellId: String,
    val items: List<ItemDiffResult>
)

interface ShelfDiffAnalyzer {
    suspend fun analyze(cells: List<CellPair>): Result<List<CellDiffResult>>
}
