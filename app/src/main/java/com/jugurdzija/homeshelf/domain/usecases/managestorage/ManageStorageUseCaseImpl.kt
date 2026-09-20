package com.jugurdzija.homeshelf.domain.usecases.managestorage

import com.jugurdzija.homeshelf.data.storage.StorageRepository
import com.jugurdzija.homeshelf.domain.model.MarkedItem
import javax.inject.Inject

class ManageStorageUseCaseImpl @Inject constructor(
    private val storageRepository: StorageRepository
) : ManageStorageUseCase {

    override suspend fun renameStorage(id: String, name: String) = storageRepository.renameStorage(id, name)

    override suspend fun deleteStorage(id: String) = storageRepository.deleteStorage(id)

    override suspend fun saveMarkedItems(id: String, items: List<MarkedItem>) =
        storageRepository.saveStorageReferenceMarkedItems(id, items)
}
