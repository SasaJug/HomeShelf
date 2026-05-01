package com.jugurdzija.homeshelf.ui.reference

import com.jugurdzija.homeshelf.data.ReferenceItem

sealed interface ReferenceListUiState {
    data object Loading : ReferenceListUiState
    data object Empty : ReferenceListUiState
    data class Loaded(val items: List<ReferenceItem>) : ReferenceListUiState
    data class Error(val message: String, val items: List<ReferenceItem>) : ReferenceListUiState
}
