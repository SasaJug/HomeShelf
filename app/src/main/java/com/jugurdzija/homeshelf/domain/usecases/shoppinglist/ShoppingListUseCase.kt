package com.jugurdzija.homeshelf.domain.usecases.shoppinglist

import com.jugurdzija.homeshelf.domain.model.ShoppingListItem

interface ShoppingListUseCase {
    suspend fun getItems(): List<ShoppingListItem>
    suspend fun getItem(id: String): ShoppingListItem?
    suspend fun addItem(name: String): ShoppingListItem
    suspend fun addAutoDetectedItems(candidates: List<Pair<String, String>>): List<ShoppingListItem>
    suspend fun removeItem(id: String)
}
