package com.jugurdzija.homeshelf.usecase

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.data.GuideLine

sealed interface ReferencePipelineResult {
    data class Done(val cellCount: Int) : ReferencePipelineResult
    data object NoCells : ReferencePipelineResult
    data class Error(val message: String) : ReferencePipelineResult
}

interface ReferencePipeline {
    suspend fun run(
        bitmap: Bitmap,
        filePath: String,
        guideLines: List<GuideLine>,
        canvasWidth: Int,
        canvasHeight: Int
    ): ReferencePipelineResult
}
