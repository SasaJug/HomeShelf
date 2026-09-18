package com.jugurdzija.homeshelf.data.pendingcapture

import android.graphics.Bitmap

interface PendingCaptureRepository {
    suspend fun save(bitmap: Bitmap)
    suspend fun load(): Bitmap?
    suspend fun clear()
}
