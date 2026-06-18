package com.jugurdzija.homeshelf.ui.reference

import com.jugurdzija.homeshelf.data.StorageItem

sealed interface ReferenceListUiState {
    data object Loading : ReferenceListUiState
    data object Empty : ReferenceListUiState
    data class Loaded(val items: List<StorageItem>) : ReferenceListUiState
    data class Error(val message: String, val items: List<StorageItem>) : ReferenceListUiState
}
