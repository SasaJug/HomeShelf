package com.jugurdzija.homeshelf.domain.usecases.managestorage

import com.jugurdzija.homeshelf.domain.model.MarkedItem

interface ManageStorageUseCase {
    suspend fun renameStorage(id: String, name: String)
    suspend fun deleteStorage(id: String)
    suspend fun saveMarkedItems(id: String, items: List<MarkedItem>)
}
