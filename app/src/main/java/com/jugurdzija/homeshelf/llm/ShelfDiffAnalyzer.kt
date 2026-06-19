package com.jugurdzija.homeshelf.llm

import android.graphics.Bitmap
import kotlinx.serialization.Serializable

data class CellPair(
    val cellId: String,
    val referenceBitmap: Bitmap,
    val newBitmap: Bitmap
)

@Serializable
data class CellDiffResult(
    val cellId: String,
    val changed: Boolean,
    val description: String
)

interface ShelfDiffAnalyzer {
    suspend fun analyze(cells: List<CellPair>): Result<List<CellDiffResult>>
}
