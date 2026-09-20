package com.jugurdzija.homeshelf.domain.usecases.shoppinglist

import com.jugurdzija.homeshelf.data.shoppinglist.ShoppingListRepository
import com.jugurdzija.homeshelf.domain.model.ShoppingListItem
import javax.inject.Inject

class ShoppingListUseCaseImpl @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository
) : ShoppingListUseCase {

    override suspend fun getItems(): List<ShoppingListItem> = shoppingListRepository.loadAll()

    override suspend fun getItem(id: String): ShoppingListItem? =
        shoppingListRepository.loadAll().firstOrNull { it.id == id }

    override suspend fun addItem(name: String): ShoppingListItem = shoppingListRepository.add(name)

    override suspend fun addAutoDetectedItems(candidates: List<Pair<String, String>>): List<ShoppingListItem> =
        shoppingListRepository.addAutoDetected(candidates)

    override suspend fun removeItem(id: String) = shoppingListRepository.remove(id)
}
