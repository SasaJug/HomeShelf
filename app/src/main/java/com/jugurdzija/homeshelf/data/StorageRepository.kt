package com.jugurdzija.homeshelf.data

import android.graphics.Bitmap
import java.io.File

interface StorageRepository {
    suspend fun loadAll(): List<StorageItem>
    suspend fun decodeLatestBitmap(id: String, sampleSize: Int = 1): Bitmap?
    suspend fun getLatestPhotoFile(id: String): File?
    suspend fun loadLatestData(id: String): ReferencePhotoData
    suspend fun createNew(name: String): StorageItem
    suspend fun saveLatest(id: String, bitmap: Bitmap, data: ReferencePhotoData, cells: List<GridCell>)
    suspend fun delete(id: String)
}
