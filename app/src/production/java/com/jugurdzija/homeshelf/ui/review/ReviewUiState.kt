package com.jugurdzija.homeshelf.ui.review

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.data.GuideLine

sealed interface ReviewUiState {
    data object Loading : ReviewUiState
    data class Done(
        val storageName: String,
        val alignedBitmap: Bitmap,
        val guideLines: List<GuideLine>,
        val similarities: Map<String, Float>
    ) : ReviewUiState
    data class CompareError(val storageName: String, val message: String) : ReviewUiState
}
