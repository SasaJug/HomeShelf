package com.jugurdzija.homeshelf.usecase

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.MarkedItem

sealed interface StorageSaveResult {
    data class Done(val storageId: String, val cellCount: Int) : StorageSaveResult
    data class Error(val message: String) : StorageSaveResult
}

interface StorageSavePipeline {
    suspend fun run(
        storageId: String?,
        name: String,
        bitmap: Bitmap,
        guideLines: List<GuideLine>,
        canvasWidth: Int,
        canvasHeight: Int,
        resolvedMarkedItems: List<MarkedItem>? = null
    ): StorageSaveResult
}
