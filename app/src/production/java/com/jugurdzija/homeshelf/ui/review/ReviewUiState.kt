package com.jugurdzija.homeshelf.ui.review

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.data.GridCell
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.MarkedItem
import com.jugurdzija.homeshelf.llm.CellDiffResult

sealed interface ReviewUiState {
    data object Loading : ReviewUiState
    data class Done(
        val storageName: String,
        val alignedBitmap: Bitmap,
        val guideLines: List<GuideLine>,
        val similarities: Map<String, Float>,
        val referenceCells: List<GridCell>,
        val newCells: List<GridCell>,
        val markedItems: List<MarkedItem>
    ) : ReviewUiState
    data class CompareError(val storageName: String, val message: String) : ReviewUiState
}

sealed interface AiDiffState {
    data object NotRequested : AiDiffState
    data object Loading : AiDiffState
    data class Done(
        val results: List<CellDiffResult>,
        val knownItemsById: Map<String, MarkedItem> = emptyMap()
    ) : AiDiffState
    data class Error(val message: String) : AiDiffState
}
