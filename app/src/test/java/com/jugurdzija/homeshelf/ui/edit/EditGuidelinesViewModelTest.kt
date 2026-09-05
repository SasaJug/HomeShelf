package com.jugurdzija.homeshelf.ui.edit

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.PendingCaptureStore
import com.jugurdzija.homeshelf.data.ReferencePhotoData
import com.jugurdzija.homeshelf.data.StorageRepository
import com.jugurdzija.homeshelf.llm.GeneratedGuideLine
import com.jugurdzija.homeshelf.llm.GridLineGenerator
import com.jugurdzija.homeshelf.ui.nav.Routes
import com.jugurdzija.homeshelf.usecase.StorageSavePipeline
import com.jugurdzija.homeshelf.usecase.StorageSaveResult
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
class EditGuidelinesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var pendingCaptureStore: PendingCaptureStore
    private lateinit var storageRepository: StorageRepository
    private lateinit var storageSavePipeline: StorageSavePipeline
    private lateinit var gridLineGenerator: GridLineGenerator
    private lateinit var bitmap: Bitmap

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        pendingCaptureStore = mockk()
        storageRepository = mockk()
        storageSavePipeline = mockk()
        gridLineGenerator = mockk()
        bitmap = mockk(relaxed = true)
        every { bitmap.width } returns 100
        every { bitmap.height } returns 100

        coEvery { pendingCaptureStore.load() } returns null
        coEvery { pendingCaptureStore.clear() } just Runs
        coEvery { storageRepository.decodeLatestBitmap(any(), any()) } returns bitmap
        coEvery { storageRepository.loadLatestData(any()) } returns ReferencePhotoData()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): EditGuidelinesViewModel {
        val savedStateHandle = SavedStateHandle(mapOf(Routes.ARG_STORAGE_ID to STORAGE_ID))
        val viewModel = EditGuidelinesViewModel(
            savedStateHandle, pendingCaptureStore, storageRepository, storageSavePipeline, gridLineGenerator
        )
        testDispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    @Test
    fun `init uses the pending capture bitmap when one exists`() = runTest(testDispatcher) {
        coEvery { pendingCaptureStore.load() } returns bitmap

        val viewModel = createViewModel()

        assertEquals(bitmap, viewModel.bitmapState.value)
        coVerify(exactly = 0) { storageRepository.decodeLatestBitmap(any(), any()) }
    }

    @Test
    fun `init falls back to the storage's latest bitmap when there is no pending capture`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        assertEquals(bitmap, viewModel.bitmapState.value)
        coVerify { storageRepository.decodeLatestBitmap(STORAGE_ID, any()) }
    }

    @Test
    fun `init loads existing guide lines and sets nextId past the highest loaded id`() = runTest(testDispatcher) {
        val lines = listOf(
            GuideLine(id = 2, isHorizontal = true, position = 0.5f),
            GuideLine(id = 5, isHorizontal = false, position = 0.25f)
        )
        coEvery { storageRepository.loadLatestData(STORAGE_ID) } returns ReferencePhotoData(guideLines = lines)

        val viewModel = createViewModel()

        assertEquals(lines, viewModel.guideLines.toList())
        assertEquals(6, viewModel.nextId)
    }

    @Test
    fun `init with no existing guide lines starts nextId at 0`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        assertEquals(emptyList<GuideLine>(), viewModel.guideLines.toList())
        assertEquals(0, viewModel.nextId)
    }

    @Test
    fun `save does nothing without a bitmap`() = runTest(testDispatcher) {
        coEvery { storageRepository.decodeLatestBitmap(any(), any()) } returns null
        val viewModel = createViewModel()

        viewModel.save(canvasWidth = 100, canvasHeight = 100)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { storageSavePipeline.run(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `save persists the current guide lines and emits Saved`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.guideLines.add(GuideLine(id = 0, isHorizontal = true, position = 0.5f))
        coEvery {
            storageSavePipeline.run(STORAGE_ID, "", bitmap, viewModel.guideLines.toList(), 100, 100)
        } returns StorageSaveResult.Done(STORAGE_ID, 3)
        val events = mutableListOf<EditNavEvent>()
        val job = launch { viewModel.navEvent.collect { events.add(it) } }

        viewModel.save(canvasWidth = 100, canvasHeight = 100)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(EditNavEvent.Saved), events)
        coVerify { pendingCaptureStore.clear() }
        job.cancel()
    }

    @Test
    fun `save surfaces a pipeline error without clearing the pending capture`() = runTest(testDispatcher) {
        coEvery {
            storageSavePipeline.run(any(), any(), any(), any(), any(), any())
        } returns StorageSaveResult.Error("boom")
        val viewModel = createViewModel()

        viewModel.save(canvasWidth = 100, canvasHeight = 100)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(StorageSaveResult.Error("boom"), viewModel.saveState.value)
        coVerify(exactly = 0) { pendingCaptureStore.clear() }
    }

    @Test
    fun `resetSaveState clears the save state`() = runTest(testDispatcher) {
        coEvery {
            storageSavePipeline.run(any(), any(), any(), any(), any(), any())
        } returns StorageSaveResult.Error("boom")
        val viewModel = createViewModel()
        viewModel.save(canvasWidth = 100, canvasHeight = 100)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.resetSaveState()

        assertNull(viewModel.saveState.value)
    }

    @Test
    fun `generateGridLines replaces the current guide lines with the generated grid`() = runTest(testDispatcher) {
        coEvery { storageRepository.loadLatestData(STORAGE_ID) } returns ReferencePhotoData(
            guideLines = listOf(GuideLine(id = 0, isHorizontal = true, position = 0.1f))
        )
        val generated = listOf(
            GeneratedGuideLine(isHorizontal = true, position = 0.5f),
            GeneratedGuideLine(isHorizontal = false, position = 0.25f)
        )
        coEvery { gridLineGenerator.generate(bitmap) } returns Result.success(generated)
        val viewModel = createViewModel()

        viewModel.generateGridLines(canvasWidth = 100, canvasHeight = 100)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            listOf(
                GuideLine(id = 1, isHorizontal = true, position = 0.5f),
                GuideLine(id = 2, isHorizontal = false, position = 0.25f)
            ),
            viewModel.guideLines.toList()
        )
        assertEquals(3, viewModel.nextId)
        assertNull(viewModel.gridGenerateState.value)
    }

    @Test
    fun `generateGridLines does nothing without a bitmap`() = runTest(testDispatcher) {
        coEvery { storageRepository.decodeLatestBitmap(any(), any()) } returns null
        val viewModel = createViewModel()

        viewModel.generateGridLines(canvasWidth = 100, canvasHeight = 100)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.gridGenerateState.value)
        coVerify(exactly = 0) { gridLineGenerator.generate(any()) }
    }

    @Test
    fun `generateGridLines surfaces a failure as an error state`() = runTest(testDispatcher) {
        coEvery { gridLineGenerator.generate(bitmap) } returns Result.failure(IllegalStateException("boom"))
        val viewModel = createViewModel()

        viewModel.generateGridLines(canvasWidth = 100, canvasHeight = 100)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(GridGenerateResult.Error("boom"), viewModel.gridGenerateState.value)
    }

    @Test
    fun `resetGridGenerateState clears the grid generate state`() = runTest(testDispatcher) {
        coEvery { gridLineGenerator.generate(bitmap) } returns Result.failure(IllegalStateException("boom"))
        val viewModel = createViewModel()
        viewModel.generateGridLines(canvasWidth = 100, canvasHeight = 100)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.resetGridGenerateState()

        assertNull(viewModel.gridGenerateState.value)
    }

    @Test
    fun `discard clears the pending capture and emits Discarded`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val events = mutableListOf<EditNavEvent>()
        val job = launch { viewModel.navEvent.collect { events.add(it) } }

        viewModel.discard()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(EditNavEvent.Discarded), events)
        coVerify { pendingCaptureStore.clear() }
        job.cancel()
    }
}
