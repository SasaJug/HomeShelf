package com.jugurdzija.homeshelf.domain.usecases.getstorage

import com.jugurdzija.homeshelf.domain.model.StorageItem

interface GetStorageUseCase {
    suspend fun getStorage(id: String): StorageItem?
    suspend fun getStorages(): List<StorageItem>
}
