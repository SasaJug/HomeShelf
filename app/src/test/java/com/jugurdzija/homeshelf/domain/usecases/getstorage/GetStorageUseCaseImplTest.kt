package com.jugurdzija.homeshelf.domain.usecases.getstorage

import com.jugurdzija.homeshelf.data.storage.StorageRepository
import com.jugurdzija.homeshelf.domain.model.StorageItem
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetStorageUseCaseImplTest {

    private val storageRepository = mockk<StorageRepository>()
    private val useCase = GetStorageUseCaseImpl(storageRepository)

    private val fridge = StorageItem(id = "1", name = "Fridge", createdAt = 0L, updatedAt = 0L)
    private val pantry = StorageItem(id = "2", name = "Pantry", createdAt = 0L, updatedAt = 0L)

    @Test
    fun `getStorage returns the storage with the matching id`() = runTest {
        coEvery { storageRepository.loadAllStorages() } returns listOf(fridge, pantry)

        assertEquals(pantry, useCase.getStorage("2"))
    }

    @Test
    fun `getStorage returns null when no storage matches`() = runTest {
        coEvery { storageRepository.loadAllStorages() } returns listOf(fridge)

        assertNull(useCase.getStorage("missing"))
    }
}
