package com.jugurdzija.homeshelf.ui.markitems

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import com.jugurdzija.homeshelf.data.BoundingBox
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.MarkedItem
import com.jugurdzija.homeshelf.data.ReferencePhotoData
import com.jugurdzija.homeshelf.data.StorageItem
import com.jugurdzija.homeshelf.data.StorageRepository
import com.jugurdzija.homeshelf.llm.DetectedItem
import com.jugurdzija.homeshelf.llm.ItemDetector
import com.jugurdzija.homeshelf.ui.nav.Routes
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val STORAGE_ID = "storage-1"

@OptIn(ExperimentalCoroutinesApi::class)
class MarkItemsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var storageRepository: StorageRepository
    private lateinit var itemDetector: ItemDetector
    private lateinit var bitmap: Bitmap

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        storageRepository = mockk()
        itemDetector = mockk()
        bitmap = mockk(relaxed = true)

        coEvery { storageRepository.loadAll() } returns listOf(
            StorageItem(id = STORAGE_ID, name = "Fridge", createdAt = 0L, updatedAt = 0L)
        )
        coEvery { storageRepository.decodeLatestBitmap(any(), any()) } returns bitmap
        coEvery { storageRepository.loadLatestData(any()) } returns ReferencePhotoData()
        coEvery { storageRepository.saveMarkedItems(any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MarkItemsViewModel {
        val savedStateHandle = SavedStateHandle(mapOf(Routes.ARG_STORAGE_ID to STORAGE_ID))
        val viewModel = MarkItemsViewModel(savedStateHandle, storageRepository, itemDetector)
        testDispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    @Test
    fun `init loads storage name and reference data`() = runTest(testDispatcher) {
        val guideLine = GuideLine(id = 1, isHorizontal = true, position = 0.5f)
        val markedItem = MarkedItem(id = "existing", name = "Rice", boundingBox = BoundingBox(0f, 0f, 0.1f, 0.1f))
        coEvery { storageRepository.loadLatestData(STORAGE_ID) } returns ReferencePhotoData(
            guideLines = listOf(guideLine),
            markedItems = listOf(markedItem)
        )

        val viewModel = createViewModel()

        assertEquals("Fridge", viewModel.storageName)
        assertEquals(listOf(guideLine), viewModel.guideLines)
        assertEquals(listOf(markedItem), viewModel.markedItems)
    }

    @Test
    fun `createItem adds a blank item and selects it`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val box = BoundingBox(0.1f, 0.1f, 0.2f, 0.2f)

        viewModel.createItem(box)

        assertEquals(1, viewModel.markedItems.size)
        val created = viewModel.markedItems.first()
        assertEquals("", created.name)
        assertEquals(box, created.boundingBox)
        assertEquals(created.id, viewModel.selectedId)
    }

    @Test
    fun `select sets the selected id`() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.select("some-id")

        assertEquals("some-id", viewModel.selectedId)
    }

    @Test
    fun `updateName updates only the matching item`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.createItem(BoundingBox(0f, 0f, 0.1f, 0.1f))
        val id = viewModel.markedItems.first().id

        viewModel.updateName(id, "Pasta")

        assertEquals("Pasta", viewModel.markedItems.first().name)
    }

    @Test
    fun `updateBoundingBox replaces the box of the matching item`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.createItem(BoundingBox(0f, 0f, 0.1f, 0.1f))
        val id = viewModel.markedItems.first().id
        val newBox = BoundingBox(0.5f, 0.5f, 0.2f, 0.2f)

        viewModel.updateBoundingBox(id, newBox)

        assertEquals(newBox, viewModel.markedItems.first().boundingBox)
    }

    @Test
    fun `updateTransparent flips the transparency flag of the matching item`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.createItem(BoundingBox(0f, 0f, 0.1f, 0.1f))
        val id = viewModel.markedItems.first().id

        viewModel.updateTransparent(id, true)

        assertEquals(true, viewModel.markedItems.first().isTransparentContainer)
    }

    @Test
    fun `confirmSelection with blank name removes the item and clears selection`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.createItem(BoundingBox(0f, 0f, 0.1f, 0.1f))

        viewModel.confirmSelection(canvasWidth = 100, canvasHeight = 100)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.markedItems.isEmpty())
        assertNull(viewModel.selectedId)
    }

    @Test
    fun `confirmSelection with a name keeps the item, clears selection and persists`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.createItem(BoundingBox(0f, 0f, 0.1f, 0.1f))
        val id = viewModel.markedItems.first().id
        viewModel.updateName(id, "Rice")

        viewModel.confirmSelection(canvasWidth = 100, canvasHeight = 100)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.markedItems.size)
        assertEquals("Rice", viewModel.markedItems.first().name)
        assertNull(viewModel.selectedId)
        coVerify { storageRepository.saveMarkedItems(STORAGE_ID, viewModel.markedItems.toList()) }
    }

    @Test
    fun `deleteItem removes the item, clears selection if selected, and persists`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.createItem(BoundingBox(0f, 0f, 0.1f, 0.1f))
        val id = viewModel.markedItems.first().id

        viewModel.deleteItem(id)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.markedItems.isEmpty())
        assertNull(viewModel.selectedId)
        coVerify { storageRepository.saveMarkedItems(STORAGE_ID, emptyList()) }
    }

    @Test
    fun `runAiDetection replaces marked items with detection results and persists`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.createItem(BoundingBox(0f, 0f, 0.1f, 0.1f))
        val detected = DetectedItem(
            name = "Rice",
            box = BoundingBox(0.1f, 0.1f, 0.2f, 0.2f),
            isTransparentContainer = false
        )
        coEvery { itemDetector.detect(any()) } returns Result.success(listOf(detected))

        viewModel.runAiDetection(canvasWidth = 100, canvasHeight = 100)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.markedItems.size)
        assertEquals("Rice", viewModel.markedItems.first().name)
        assertNull(viewModel.detectState.value)
        coVerify { storageRepository.saveMarkedItems(STORAGE_ID, viewModel.markedItems.toList()) }
    }

    @Test
    fun `runAiDetection surfaces failure as an error state and keeps existing items`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.createItem(BoundingBox(0f, 0f, 0.1f, 0.1f))
        viewModel.updateName(viewModel.markedItems.first().id, "Existing")
        coEvery { itemDetector.detect(any()) } returns Result.failure(IllegalStateException("boom"))

        viewModel.runAiDetection(canvasWidth = 100, canvasHeight = 100)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(DetectState.Error("boom"), viewModel.detectState.value)
        assertEquals(1, viewModel.markedItems.size)
        assertEquals("Existing", viewModel.markedItems.first().name)
    }

    @Test
    fun `runAiDetection ignores concurrent calls while a detection is already in flight`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        coEvery { itemDetector.detect(any()) } returns Result.success(emptyList())

        viewModel.runAiDetection(canvasWidth = 100, canvasHeight = 100)
        viewModel.runAiDetection(canvasWidth = 100, canvasHeight = 100)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { itemDetector.detect(any()) }
    }

    @Test
    fun `resetDetectState clears the detect state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        coEvery { itemDetector.detect(any()) } returns Result.failure(IllegalStateException("boom"))
        viewModel.runAiDetection(canvasWidth = 100, canvasHeight = 100)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.resetDetectState()

        assertNull(viewModel.detectState.value)
    }
}
