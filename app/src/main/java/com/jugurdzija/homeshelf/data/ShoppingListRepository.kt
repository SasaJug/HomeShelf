package com.jugurdzija.homeshelf.data

interface ShoppingListRepository {
    suspend fun loadAll(): List<ShoppingListItem>
    suspend fun add(name: String, storageId: String? = null): ShoppingListItem
    suspend fun addAutoDetected(candidates: List<Pair<String, String>>): List<ShoppingListItem>
    suspend fun remove(id: String)
}
