package com.jugurdzija.homeshelf.data

import android.graphics.Bitmap

interface StorageStore {
    suspend fun loadAll(): List<StorageItem>
    suspend fun decodeLatestBitmap(id: String, sampleSize: Int = 1): Bitmap?
    suspend fun loadLatestData(id: String): ReferencePhotoData
    suspend fun createNew(name: String): StorageItem
    suspend fun saveLatest(id: String, bitmap: Bitmap, data: ReferencePhotoData, cells: List<GridCell>)
}
