package com.jugurdzija.homeshelf.ui.compare

import com.jugurdzija.homeshelf.embedding.ReferenceMatch

sealed interface CompareUiState {
    data object Loading : CompareUiState
    data class MissingReference(val message: String) : CompareUiState
    data object PermissionDenied : CompareUiState
    data class Streaming(val matches: List<ReferenceMatch>) : CompareUiState
    data class Error(val message: String, val matches: List<ReferenceMatch>) : CompareUiState
}
