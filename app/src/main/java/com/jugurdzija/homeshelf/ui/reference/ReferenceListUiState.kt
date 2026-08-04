package com.jugurdzija.homeshelf.ui.reference

import com.jugurdzija.homeshelf.data.StorageListEntry

sealed interface ReferenceListUiState {
    data object Loading : ReferenceListUiState
    data object Empty : ReferenceListUiState
    data class Loaded(val items: List<StorageListEntry>) : ReferenceListUiState
    data class Error(val message: String, val items: List<StorageListEntry>) : ReferenceListUiState
}
