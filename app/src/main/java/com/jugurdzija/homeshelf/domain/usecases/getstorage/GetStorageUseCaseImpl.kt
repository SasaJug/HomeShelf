package com.jugurdzija.homeshelf.domain.usecases.getstorage

import com.jugurdzija.homeshelf.data.storage.StorageRepository
import com.jugurdzija.homeshelf.domain.model.StorageItem
import javax.inject.Inject

class GetStorageUseCaseImpl @Inject constructor(
    private val storageRepository: StorageRepository
) : GetStorageUseCase {

    override suspend fun getStorages(): List<StorageItem> = storageRepository.loadAllStorages()

    override suspend fun getStorage(id: String): StorageItem? =
        storageRepository.loadAllStorages().firstOrNull { it.id == id }
}
