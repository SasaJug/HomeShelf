package com.jugurdzija.homeshelf.ui.scan

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import com.jugurdzija.homeshelf.domain.model.GuideLine
import com.jugurdzija.homeshelf.domain.usecases.pendingcapture.PendingCaptureUseCase
import com.jugurdzija.homeshelf.domain.model.ReferencePhotoData
import com.jugurdzija.homeshelf.domain.model.StorageItem
import com.jugurdzija.homeshelf.domain.usecases.getstorage.GetStorageUseCase
import com.jugurdzija.homeshelf.domain.usecases.getstoragereference.GetStorageReferenceUseCase
import com.jugurdzija.homeshelf.aipipeline.embedding.EmbedderOwner
import com.jugurdzija.homeshelf.aipipeline.embedding.ReferenceMatch
import com.jugurdzija.homeshelf.ui.nav.Routes
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private const val STORAGE_ID = "storage-1"

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getStorageUseCase: GetStorageUseCase
    private lateinit var getStorageReferenceUseCase: GetStorageReferenceUseCase
    private lateinit var embedder: EmbedderOwner
    private lateinit var pendingCaptureUseCase: PendingCaptureUseCase
    private lateinit var errorsFlow: MutableSharedFlow<String>
    private lateinit var bitmap: Bitmap

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getStorageUseCase = mockk()
        getStorageReferenceUseCase = mockk()
        embedder = mockk()
        pendingCaptureUseCase = mockk()
        errorsFlow = MutableSharedFlow(extraBufferCapacity = 1)
        bitmap = mockk(relaxed = true)

        stubStorages(emptyList())
        coEvery { getStorageReferenceUseCase.getBitmap(any()) } returns null
        coEvery { getStorageReferenceUseCase.getData(any()) } returns ReferencePhotoData()
        coEvery { pendingCaptureUseCase.save(any()) } just Runs
        every { embedder.errors } returns errorsFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun stubStorages(items: List<StorageItem>) {
        coEvery { getStorageUseCase.getStorages() } returns items
        coEvery { getStorageUseCase.getStorage(any()) } answers { items.firstOrNull { it.id == firstArg() } }
    }

    private fun createViewModel(pinnedStorageId: String? = null, rescan: Boolean = false): ScanViewModel {
        val savedStateHandle = SavedStateHandle(
            buildMap {
                pinnedStorageId?.let { put(Routes.ARG_STORAGE_ID, it) }
                if (rescan) put(Routes.ARG_MODE, Routes.MODE_RESCAN)
            }
        )
        val viewModel = ScanViewModel(savedStateHandle, getStorageUseCase, getStorageReferenceUseCase, embedder, pendingCaptureUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    @Test
    fun `init with no pinned storage loads reference bitmaps and reflects an empty Streaming state`() = runTest(testDispatcher) {
        val item = StorageItem(id = STORAGE_ID, name = "Fridge", createdAt = 0L, updatedAt = 0L)
        stubStorages(listOf(item))
        coEvery { getStorageReferenceUseCase.getBitmap(STORAGE_ID) } returns bitmap

        val viewModel = createViewModel()

        assertEquals(ScanUiState.Streaming(), viewModel.state.value)
    }

    @Test
    fun `init with no pinned storage drops storages that have no bitmap`() = runTest(testDispatcher) {
        val withBitmap = StorageItem(id = "with-bitmap", name = "Fridge", createdAt = 0L, updatedAt = 0L)
        val withoutBitmap = StorageItem(id = "without-bitmap", name = "Pantry", createdAt = 0L, updatedAt = 0L)
        stubStorages(listOf(withBitmap, withoutBitmap))
        coEvery { getStorageReferenceUseCase.getBitmap("with-bitmap") } returns bitmap
        coEvery { getStorageReferenceUseCase.getBitmap("without-bitmap") } returns null
        coEvery { embedder.embedAll(any(), any(), any()) } returns emptyList()

        val viewModel = createViewModel()
        viewModel.onFrameReceived(bitmap)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { embedder.embedAll(bitmap, listOf(withBitmap to bitmap), any()) }
    }

    @Test
    fun `init with a pinned storage loads its name and cached guide lines`() = runTest(testDispatcher) {
        val item = StorageItem(id = STORAGE_ID, name = "Fridge", createdAt = 0L, updatedAt = 0L)
        val guideLine = GuideLine(id = 1, isHorizontal = true, position = 0.5f)
        stubStorages(listOf(item))
        coEvery { getStorageReferenceUseCase.getData(STORAGE_ID) } returns ReferencePhotoData(guideLines = listOf(guideLine))

        val viewModel = createViewModel(pinnedStorageId = STORAGE_ID)

        assertEquals(ScanUiState.Streaming(detected = item, guideLines = listOf(guideLine)), viewModel.state.value)
    }

    @Test
    fun `onFrameReceived is ignored while the initial state is still Loading`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle()
        coEvery { embedder.embedAll(any(), any(), any()) } returns emptyList()
        val viewModel = ScanViewModel(savedStateHandle, getStorageUseCase, getStorageReferenceUseCase, embedder, pendingCaptureUseCase)

        viewModel.onFrameReceived(bitmap)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { embedder.embedAll(any(), any(), any()) }
    }

    @Test
    fun `onFrameReceived is ignored when a storage is pinned`() = runTest(testDispatcher) {
        val item = StorageItem(id = STORAGE_ID, name = "Fridge", createdAt = 0L, updatedAt = 0L)
        stubStorages(listOf(item))
        val viewModel = createViewModel(pinnedStorageId = STORAGE_ID)

        viewModel.onFrameReceived(bitmap)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { embedder.embedAll(any(), any(), any()) }
    }

    @Test
    fun `onFrameReceived sets the detected storage when the best match is above the threshold`() = runTest(testDispatcher) {
        val item = StorageItem(id = STORAGE_ID, name = "Fridge", createdAt = 0L, updatedAt = 0L)
        val guideLine = GuideLine(id = 1, isHorizontal = true, position = 0.5f)
        val matches = listOf(ReferenceMatch(item, similarity = 0.9, inferenceMs = 10))
        stubStorages(listOf(item))
        coEvery { getStorageReferenceUseCase.getBitmap(STORAGE_ID) } returns bitmap
        coEvery { getStorageReferenceUseCase.getData(STORAGE_ID) } returns ReferencePhotoData(guideLines = listOf(guideLine))
        coEvery { embedder.embedAll(any(), any(), any()) } returns matches
        val viewModel = createViewModel()

        viewModel.onFrameReceived(bitmap)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ScanUiState.Streaming(matches, item, listOf(guideLine)), viewModel.state.value)
    }

    @Test
    fun `onFrameReceived clears the detected storage when the best match is below the threshold`() = runTest(testDispatcher) {
        val item = StorageItem(id = STORAGE_ID, name = "Fridge", createdAt = 0L, updatedAt = 0L)
        val matches = listOf(ReferenceMatch(item, similarity = 0.3, inferenceMs = 10))
        stubStorages(listOf(item))
        coEvery { getStorageReferenceUseCase.getBitmap(STORAGE_ID) } returns bitmap
        coEvery { embedder.embedAll(any(), any(), any()) } returns matches
        val viewModel = createViewModel()

        viewModel.onFrameReceived(bitmap)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ScanUiState.Streaming(matches, null, emptyList()), viewModel.state.value)
    }

    @Test
    fun `onFrameReceived resets to an empty Streaming state when there are no reference bitmaps`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onFrameReceived(bitmap)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ScanUiState.Streaming(), viewModel.state.value)
        coVerify(exactly = 0) { embedder.embedAll(any(), any(), any()) }
    }

    @Test
    fun `onFrameReceived ignores concurrent calls while inference is already in flight`() = runTest(testDispatcher) {
        val item = StorageItem(id = STORAGE_ID, name = "Fridge", createdAt = 0L, updatedAt = 0L)
        stubStorages(listOf(item))
        coEvery { getStorageReferenceUseCase.getBitmap(STORAGE_ID) } returns bitmap
        coEvery { embedder.embedAll(any(), any(), any()) } returns emptyList()
        val viewModel = createViewModel()

        viewModel.onFrameReceived(bitmap)
        viewModel.onFrameReceived(bitmap)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { embedder.embedAll(any(), any(), any()) }
    }

    @Test
    fun `onCaptureBitmap navigates to confirm for a rescan of a pinned storage`() = runTest(testDispatcher) {
        val item = StorageItem(id = STORAGE_ID, name = "Fridge", createdAt = 0L, updatedAt = 0L)
        stubStorages(listOf(item))
        val viewModel = createViewModel(pinnedStorageId = STORAGE_ID, rescan = true)
        val events = mutableListOf<ScanNavEvent>()
        val job = launch { viewModel.navEvent.collect { events.add(it) } }

        viewModel.onCaptureBitmap(bitmap)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(ScanNavEvent.ToConfirm(STORAGE_ID)), events)
        coVerify { pendingCaptureUseCase.save(bitmap) }
        job.cancel()
    }

    @Test
    fun `onCaptureBitmap navigates to review for a pinned storage that is not a rescan`() = runTest(testDispatcher) {
        val item = StorageItem(id = STORAGE_ID, name = "Fridge", createdAt = 0L, updatedAt = 0L)
        stubStorages(listOf(item))
        val viewModel = createViewModel(pinnedStorageId = STORAGE_ID, rescan = false)
        val events = mutableListOf<ScanNavEvent>()
        val job = launch { viewModel.navEvent.collect { events.add(it) } }

        viewModel.onCaptureBitmap(bitmap)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(ScanNavEvent.ToReview(STORAGE_ID)), events)
        job.cancel()
    }

    @Test
    fun `onCaptureBitmap navigates to review for the detected storage when nothing is pinned`() = runTest(testDispatcher) {
        val item = StorageItem(id = STORAGE_ID, name = "Fridge", createdAt = 0L, updatedAt = 0L)
        val matches = listOf(ReferenceMatch(item, similarity = 0.9, inferenceMs = 10))
        stubStorages(listOf(item))
        coEvery { getStorageReferenceUseCase.getBitmap(STORAGE_ID) } returns bitmap
        coEvery { embedder.embedAll(any(), any(), any()) } returns matches
        val viewModel = createViewModel()
        viewModel.onFrameReceived(bitmap)
        testDispatcher.scheduler.advanceUntilIdle()
        val events = mutableListOf<ScanNavEvent>()
        val job = launch { viewModel.navEvent.collect { events.add(it) } }

        viewModel.onCaptureBitmap(bitmap)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(ScanNavEvent.ToReview(STORAGE_ID)), events)
        job.cancel()
    }

    @Test
    fun `onCaptureBitmap navigates to confirm with no storage when nothing is pinned or detected`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val events = mutableListOf<ScanNavEvent>()
        val job = launch { viewModel.navEvent.collect { events.add(it) } }

        viewModel.onCaptureBitmap(bitmap)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(ScanNavEvent.ToConfirm(null)), events)
        job.cancel()
    }

    @Test
    fun `onPermissionDenied sets an Error state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onPermissionDenied()

        assertEquals(ScanUiState.Error("Camera permission is required", null, emptyList()), viewModel.state.value)
    }

    @Test
    fun `an embedder error updates the state to Error while preserving the detected storage and guide lines`() = runTest(testDispatcher) {
        val item = StorageItem(id = STORAGE_ID, name = "Fridge", createdAt = 0L, updatedAt = 0L)
        val guideLine = GuideLine(id = 1, isHorizontal = true, position = 0.5f)
        val matches = listOf(ReferenceMatch(item, similarity = 0.9, inferenceMs = 10))
        stubStorages(listOf(item))
        coEvery { getStorageReferenceUseCase.getBitmap(STORAGE_ID) } returns bitmap
        coEvery { getStorageReferenceUseCase.getData(STORAGE_ID) } returns ReferencePhotoData(guideLines = listOf(guideLine))
        coEvery { embedder.embedAll(any(), any(), any()) } returns matches
        val viewModel = createViewModel()
        viewModel.onFrameReceived(bitmap)
        testDispatcher.scheduler.advanceUntilIdle()

        errorsFlow.emit("Model failed to load")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            ScanUiState.Error("Model failed to load", item, listOf(guideLine)),
            viewModel.state.value
        )
    }
}
