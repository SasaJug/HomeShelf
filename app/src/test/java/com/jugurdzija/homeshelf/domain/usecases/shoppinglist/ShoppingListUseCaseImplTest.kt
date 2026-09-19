package com.jugurdzija.homeshelf.domain.usecases.shoppinglist

import com.jugurdzija.homeshelf.data.shoppinglist.ShoppingListRepository
import com.jugurdzija.homeshelf.domain.model.ShoppingListItem
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShoppingListUseCaseImplTest {

    private val shoppingListRepository = mockk<ShoppingListRepository>()
    private val useCase = ShoppingListUseCaseImpl(shoppingListRepository)

    private val milk = ShoppingListItem(id = "1", name = "Milk", createdAt = 0L)
    private val eggs = ShoppingListItem(id = "2", name = "Eggs", createdAt = 0L)

    @Test
    fun `getItem returns the item with the matching id`() = runTest {
        coEvery { shoppingListRepository.loadAll() } returns listOf(milk, eggs)

        assertEquals(eggs, useCase.getItem("2"))
    }

    @Test
    fun `getItem returns null when no item matches`() = runTest {
        coEvery { shoppingListRepository.loadAll() } returns listOf(milk)

        assertNull(useCase.getItem("missing"))
    }
}
