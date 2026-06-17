package com.jugurdzija.homeshelf.usecase

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.data.GuideLine

sealed interface ComparisonResult {
    data class Success(
        val alignedBitmap: Bitmap,
        val guideLines: List<GuideLine>,
        val similarities: Map<String, Float>
    ) : ComparisonResult
    data object AlignmentFailed : ComparisonResult
    data object NoGuideLines : ComparisonResult
    data object NoEmbeddings : ComparisonResult
    data object NoCells : ComparisonResult
}

interface ComparisonPipeline {
    suspend fun run(capturedBitmap: Bitmap, storageId: String): ComparisonResult
}
