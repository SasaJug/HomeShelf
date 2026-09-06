package com.jugurdzija.homeshelf.ui.confirm

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import com.jugurdzija.homeshelf.data.PendingCaptureStore
import com.jugurdzija.homeshelf.data.StorageItem
import com.jugurdzija.homeshelf.data.StorageRepository
import com.jugurdzija.homeshelf.ui.nav.Routes
import com.jugurdzija.homeshelf.usecase.StorageSavePipeline
import com.jugurdzija.homeshelf.usecase.StorageSaveResult
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

private const val STORAGE_ID = "storage-1"

@OptIn(ExperimentalCoroutinesApi::class)
class ConfirmCaptureViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var pendingCaptureStore: PendingCaptureStore
    private lateinit var storageRepository: StorageRepository
    private lateinit var storageSavePipeline: StorageSavePipeline
    private lateinit var bitmap: Bitmap

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        pendingCaptureStore = mockk()
        storageRepository = mockk()
        storageSavePipeline = mockk()
        bitmap = mockk(relaxed = true)

        coEvery { pendingCaptureStore.load() } returns bitmap
        coEvery { pendingCaptureStore.clear() } just Runs
        coEvery { storageRepository.loadAll() } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(pinnedStorageId: String? = null): ConfirmCaptureViewModel {
        val savedStateHandle = SavedStateHandle(
            buildMap { pinnedStorageId?.let { put(Routes.ARG_STORAGE_ID, it) } }
        )
        val viewModel = ConfirmCaptureViewModel(savedStateHandle, pendingCaptureStore, storageRepository, storageSavePipeline)
        testDispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    @Test
    fun `init loads the pending capture bitmap for a new storage`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        assertEquals(bitmap, viewModel.bitmapState.value)
        assertNull(viewModel.existingName.value)
    }

    @Test
    fun `init with a pinned storage loads its existing name`() = runTest(testDispatcher) {
        val item = StorageItem(id = STORAGE_ID, name = "Fridge", createdAt = 0L, updatedAt = 0L)
        coEvery { storageRepository.loadAll() } returns listOf(item)

        val viewModel = createViewModel(pinnedStorageId = STORAGE_ID)

        assertEquals(true, viewModel.isRescan)
        assertEquals("Fridge", viewModel.existingName.value)
    }

    @Test
    fun `save does nothing when there is no bitmap yet`() = runTest(testDispatcher) {
        coEvery { pendingCaptureStore.load() } returns null
        val viewModel = createViewModel()
        viewModel.name = "Fridge"

        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { storageSavePipeline.run(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `save ignores a blank name for a new storage`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.name = "   "

        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { storageSavePipeline.run(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `save trims the name and creates a new storage`() = runTest(testDispatcher) {
        coEvery {
            storageSavePipeline.run(null, "Fridge", bitmap, emptyList(), bitmap.width, bitmap.height, emptyList())
        } returns StorageSaveResult.Done("new-id", 0)
        val viewModel = createViewModel()
        viewModel.name = "  Fridge  "
        val events = mutableListOf<ConfirmCaptureNavEvent>()
        val job = launch { viewModel.navEvent.collect { events.add(it) } }

        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(ConfirmCaptureNavEvent.Saved("new-id")), events)
        coVerify { pendingCaptureStore.clear() }
        job.cancel()
    }

    @Test
    fun `save uses the existing name for a rescan regardless of the name field`() = runTest(testDispatcher) {
        val item = StorageItem(id = STORAGE_ID, name = "Fridge", createdAt = 0L, updatedAt = 0L)
        coEvery { storageRepository.loadAll() } returns listOf(item)
        coEvery {
            storageSavePipeline.run(STORAGE_ID, "Fridge", bitmap, emptyList(), bitmap.width, bitmap.height, emptyList())
        } returns StorageSaveResult.Done(STORAGE_ID, 0)
        val viewModel = createViewModel(pinnedStorageId = STORAGE_ID)
        val events = mutableListOf<ConfirmCaptureNavEvent>()
        val job = launch { viewModel.navEvent.collect { events.add(it) } }

        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(ConfirmCaptureNavEvent.Saved(STORAGE_ID)), events)
        job.cancel()
    }

    @Test
    fun `save surfaces a pipeline error without clearing the pending capture`() = runTest(testDispatcher) {
        coEvery {
            storageSavePipeline.run(any(), any(), any(), any(), any(), any(), any())
        } returns StorageSaveResult.Error("boom")
        val viewModel = createViewModel()
        viewModel.name = "Fridge"

        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(StorageSaveResult.Error("boom"), viewModel.saveState.value)
        coVerify(exactly = 0) { pendingCaptureStore.clear() }
    }

    @Test
    fun `resetSaveState clears the save state`() = runTest(testDispatcher) {
        coEvery {
            storageSavePipeline.run(any(), any(), any(), any(), any(), any(), any())
        } returns StorageSaveResult.Error("boom")
        val viewModel = createViewModel()
        viewModel.name = "Fridge"
        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.resetSaveState()

        assertNull(viewModel.saveState.value)
    }

    @Test
    fun `discard clears the pending capture and emits Discarded`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val events = mutableListOf<ConfirmCaptureNavEvent>()
        val job = launch { viewModel.navEvent.collect { events.add(it) } }

        viewModel.discard()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(ConfirmCaptureNavEvent.Discarded), events)
        coVerify { pendingCaptureStore.clear() }
        job.cancel()
    }
}
