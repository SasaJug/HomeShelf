package com.jugurdzija.homeshelf.domain.usecases.pendingcapture

import android.graphics.Bitmap

interface PendingCaptureUseCase {
    suspend fun save(bitmap: Bitmap)
    suspend fun load(): Bitmap?
    suspend fun clear()
}
