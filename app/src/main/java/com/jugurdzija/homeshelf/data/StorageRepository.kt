package com.jugurdzija.homeshelf.data

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.domain.model.GridCell
import com.jugurdzija.homeshelf.domain.model.MarkedItem
import com.jugurdzija.homeshelf.domain.model.ReferencePhotoData
import com.jugurdzija.homeshelf.domain.model.StorageItem
import java.io.File

interface StorageRepository {
    suspend fun loadAll(): List<StorageItem>
    suspend fun decodeLatestBitmap(id: String, sampleSize: Int = 1): Bitmap?
    suspend fun getLatestPhotoFile(id: String): File?
    suspend fun loadLatestData(id: String): ReferencePhotoData
    suspend fun createNew(name: String): StorageItem
    suspend fun saveLatest(id: String, bitmap: Bitmap, data: ReferencePhotoData, cells: List<GridCell>)
    suspend fun saveMarkedItems(id: String, items: List<MarkedItem>)
    suspend fun rename(id: String, name: String)
    suspend fun delete(id: String)
}
