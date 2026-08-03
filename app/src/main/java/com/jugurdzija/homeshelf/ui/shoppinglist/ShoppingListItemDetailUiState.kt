package com.jugurdzija.homeshelf.ui.shoppinglist

sealed interface ShoppingListItemDetailUiState {
    data object Loading : ShoppingListItemDetailUiState
    data class Loaded(val itemName: String, val storageName: String?) : ShoppingListItemDetailUiState
}
