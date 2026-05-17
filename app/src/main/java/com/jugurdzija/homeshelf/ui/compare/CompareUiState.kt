package com.jugurdzija.homeshelf.ui.compare

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.embedding.ReferenceMatch

sealed interface CompareUiState {
    data object Loading : CompareUiState
    data class MissingReference(val message: String) : CompareUiState
    data object PermissionDenied : CompareUiState
    data class Streaming(val matches: List<ReferenceMatch>, val guideLines: List<GuideLine> = emptyList()) : CompareUiState
    data class Error(val message: String, val matches: List<ReferenceMatch>, val guideLines: List<GuideLine> = emptyList()) : CompareUiState
    data class CapturePending(val matches: List<ReferenceMatch>, val guideLines: List<GuideLine> = emptyList()) : CompareUiState
    data class Captured(val frameBitmap: Bitmap, val matches: List<ReferenceMatch>, val guideLines: List<GuideLine> = emptyList()) : CompareUiState
    data class Aligned(val alignedBitmap: Bitmap, val referenceBitmap: Bitmap, val matches: List<ReferenceMatch>, val guideLines: List<GuideLine> = emptyList(), val referenceFilePath: String = "") : CompareUiState
}
