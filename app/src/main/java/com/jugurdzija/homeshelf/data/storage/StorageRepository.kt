package com.jugurdzija.homeshelf.data.storage

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.domain.model.MarkedItem
import com.jugurdzija.homeshelf.domain.model.ReferencePhotoData
import com.jugurdzija.homeshelf.domain.model.StorageItem

interface StorageRepository {
    suspend fun loadAllStorages(): List<StorageItem>
    suspend fun getStorageReferenceBitmap(id: String): Bitmap?
    suspend fun getStorageReferenceThumbnail(id: String): Bitmap?
    suspend fun loadStorageReferenceData(id: String): ReferencePhotoData
    suspend fun createStorage(name: String): StorageItem
    suspend fun saveStorageReference(id: String, bitmap: Bitmap, data: ReferencePhotoData)
    suspend fun saveStorageReferenceMarkedItems(id: String, items: List<MarkedItem>)
    suspend fun renameStorage(id: String, name: String)
    suspend fun deleteStorage(id: String)
}
