package com.jugurdzija.homeshelf.ui.shoppinglist

import com.jugurdzija.homeshelf.data.ShoppingListItem
import com.jugurdzija.homeshelf.data.ShoppingListRepository
import com.jugurdzija.homeshelf.stt.AudioRecorder
import com.jugurdzija.homeshelf.stt.SpeechToTextEngine
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var shoppingListRepository: ShoppingListRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        shoppingListRepository = mockk()
        coEvery { shoppingListRepository.loadAll() } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ShoppingListViewModel {
        val viewModel = ShoppingListViewModel(shoppingListRepository, mockk<AudioRecorder>(), mockk<SpeechToTextEngine>())
        testDispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    @Test
    fun `init with no items reflects Empty state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        assertEquals(ShoppingListUiState.Empty, viewModel.state.value)
    }

    @Test
    fun `init with items reflects Loaded state`() = runTest(testDispatcher) {
        val items = listOf(ShoppingListItem(id = "1", name = "Milk", createdAt = 0L))
        coEvery { shoppingListRepository.loadAll() } returns items

        val viewModel = createViewModel()

        assertEquals(ShoppingListUiState.Loaded(items), viewModel.state.value)
    }

    @Test
    fun `onAdd trims name, adds it and reloads`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        coEvery { shoppingListRepository.add("Eggs", null) } returns ShoppingListItem(id = "1", name = "Eggs", createdAt = 0L)

        viewModel.onNewItemNameChange("  Eggs  ")
        viewModel.onAdd()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { shoppingListRepository.add("Eggs", null) }
    }

    @Test
    fun `onAdd ignores blank input`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onNewItemNameChange("   ")
        viewModel.onAdd()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { shoppingListRepository.add(any(), any()) }
    }

    @Test
    fun `onRemove removes the item and reloads`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        coEvery { shoppingListRepository.remove("1") } just Runs

        viewModel.onRemove("1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { shoppingListRepository.remove("1") }
        assertTrue(viewModel.state.value is ShoppingListUiState.Empty)
    }
}
