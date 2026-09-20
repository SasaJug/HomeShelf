package com.jugurdzija.homeshelf.domain.usecases.getstorageoverview

import com.jugurdzija.homeshelf.data.storage.StorageRepository
import com.jugurdzija.homeshelf.domain.model.BoundingBox
import com.jugurdzija.homeshelf.domain.model.GuideLine
import com.jugurdzija.homeshelf.domain.model.MarkedItem
import com.jugurdzija.homeshelf.domain.model.ReferencePhotoData
import com.jugurdzija.homeshelf.domain.model.StorageCompleteness
import com.jugurdzija.homeshelf.domain.model.StorageItem
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetStorageOverviewUseCaseImplTest {

    private val storageRepository = mockk<StorageRepository>()
    private val useCase = GetStorageOverviewUseCaseImpl(storageRepository)

    private val fridge = StorageItem(id = "1", name = "Fridge", createdAt = 0L, updatedAt = 0L)
    private val pantry = StorageItem(id = "2", name = "Pantry", createdAt = 0L, updatedAt = 0L)

    private val guideLines = listOf(GuideLine(id = 1, isHorizontal = true, position = 0.5f))
    private val markedItems = listOf(MarkedItem(id = "1", name = "Rice", boundingBox = BoundingBox(0f, 0f, 0.1f, 0.1f)))

    private suspend fun completenessOf(data: ReferencePhotoData): StorageCompleteness {
        coEvery { storageRepository.loadAllStorages() } returns listOf(fridge)
        coEvery { storageRepository.loadStorageReferenceData("1") } returns data
        return useCase.getOverview().single().completeness
    }

    @Test
    fun `getOverview returns empty list when there are no storages`() = runTest {
        coEvery { storageRepository.loadAllStorages() } returns emptyList()

        assertTrue(useCase.getOverview().isEmpty())
    }

    @Test
    fun `getOverview pairs each storage with its own completeness`() = runTest {
        coEvery { storageRepository.loadAllStorages() } returns listOf(fridge, pantry)
        coEvery { storageRepository.loadStorageReferenceData("1") } returns ReferencePhotoData()
        coEvery { storageRepository.loadStorageReferenceData("2") } returns
            ReferencePhotoData(guideLines = guideLines, markedItems = markedItems)

        val overview = useCase.getOverview()

        assertEquals(listOf(fridge, pantry), overview.map { it.item })
        assertEquals(
            listOf(StorageCompleteness.NO_ITEMS, StorageCompleteness.COMPLETE),
            overview.map { it.completeness }
        )
    }

    @Test
    fun `completeness is COMPLETE when both grid and items exist`() = runTest {
        assertEquals(
            StorageCompleteness.COMPLETE,
            completenessOf(ReferencePhotoData(guideLines = guideLines, markedItems = markedItems))
        )
    }

    @Test
    fun `completeness is NO_GRID when items exist but no guide lines`() = runTest {
        assertEquals(
            StorageCompleteness.NO_GRID,
            completenessOf(ReferencePhotoData(guideLines = emptyList(), markedItems = markedItems))
        )
    }

    @Test
    fun `completeness is NO_ITEMS when no items exist regardless of grid`() = runTest {
        assertEquals(
            StorageCompleteness.NO_ITEMS,
            completenessOf(ReferencePhotoData(guideLines = guideLines, markedItems = emptyList()))
        )
    }

    @Test
    fun `completeness prefers NO_ITEMS when both grid and items are missing`() = runTest {
        assertEquals(
            StorageCompleteness.NO_ITEMS,
            completenessOf(ReferencePhotoData(guideLines = emptyList(), markedItems = emptyList()))
        )
    }
}
