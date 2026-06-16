package com.jugurdzija.homeshelf.data

import android.graphics.Bitmap

interface PendingCaptureStore {
    suspend fun save(bitmap: Bitmap)
    suspend fun load(): Bitmap?
    suspend fun clear()
}
