package com.jugurdzija.homeshelf.usecase

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.data.GridCell
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.MarkedItem

sealed interface ComparisonResult {
    data class Success(
        val alignedBitmap: Bitmap,
        val guideLines: List<GuideLine>,
        val similarities: Map<String, Float>,
        val referenceCells: List<GridCell>,
        val newCells: List<GridCell>,
        val markedItems: List<MarkedItem>
    ) : ComparisonResult
    data object AlignmentFailed : ComparisonResult
    data object NoEmbeddings : ComparisonResult
    data object NoCells : ComparisonResult
}

interface ComparisonPipeline {
    suspend fun run(capturedBitmap: Bitmap, storageId: String): ComparisonResult
}
