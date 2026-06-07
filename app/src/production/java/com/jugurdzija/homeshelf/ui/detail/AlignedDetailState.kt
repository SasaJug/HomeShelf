package com.jugurdzija.homeshelf.ui.detail

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.data.GuideLine

sealed class AlignedDetailState {
    object Idle : AlignedDetailState()
    object Processing : AlignedDetailState()
    object NoReference : AlignedDetailState()
    object AlignmentFailed : AlignedDetailState()
    object NoCells : AlignedDetailState()
    data class Done(
        val alignedBitmap: Bitmap,
        val guideLines: List<GuideLine>,
        val similarities: Map<String, Float>
    ) : AlignedDetailState()
    data class Error(val message: String) : AlignedDetailState()
}
