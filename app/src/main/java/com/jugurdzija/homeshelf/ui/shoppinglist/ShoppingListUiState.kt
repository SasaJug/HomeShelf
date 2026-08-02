package com.jugurdzija.homeshelf.ui.shoppinglist

import com.jugurdzija.homeshelf.data.ShoppingListItem

sealed interface ShoppingListUiState {
    data object Loading : ShoppingListUiState
    data object Empty : ShoppingListUiState
    data class Loaded(val items: List<ShoppingListItem>) : ShoppingListUiState
    data class Error(val message: String, val items: List<ShoppingListItem>) : ShoppingListUiState
}
