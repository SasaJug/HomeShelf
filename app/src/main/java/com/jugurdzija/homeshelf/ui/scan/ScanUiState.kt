package com.jugurdzija.homeshelf.ui.scan

import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.StorageItem
import com.jugurdzija.homeshelf.embedding.ReferenceMatch

sealed interface ScanUiState {
    data object Loading : ScanUiState

    data class Streaming(
        val matches: List<ReferenceMatch> = emptyList(),
        val detected: StorageItem? = null,
        val guideLines: List<GuideLine> = emptyList()
    ) : ScanUiState

    data class Error(
        val message: String,
        val detected: StorageItem?,
        val guideLines: List<GuideLine>
    ) : ScanUiState
}
