package com.jugurdzija.homeshelf.data

import android.graphics.Bitmap

interface ReferenceImageStore {
    suspend fun loadAll(): List<ReferenceItem>
    suspend fun saveReference(bitmap: Bitmap): ReferenceItem
    suspend fun delete(id: String): Boolean
    suspend fun decodeBitmap(item: ReferenceItem, sampleSize: Int = 1): Bitmap?
}
