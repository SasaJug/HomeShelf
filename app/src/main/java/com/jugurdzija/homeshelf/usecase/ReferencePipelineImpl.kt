package com.jugurdzija.homeshelf.usecase

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.data.GridCellStore
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.ReferenceDataStore
import com.jugurdzija.homeshelf.data.ReferencePhotoData
import com.jugurdzija.homeshelf.embedding.GridCellEmbedder
import com.jugurdzija.homeshelf.homography.GridProcessor
import com.jugurdzija.homeshelf.util.mapLinesToImageCoords
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferencePipelineImpl @Inject constructor(
    private val referenceDataStore: ReferenceDataStore,
    private val gridCellStore: GridCellStore,
    private val gridProcessor: GridProcessor,
    private val gridCellEmbedder: GridCellEmbedder
) : ReferencePipeline {

    override suspend fun run(
        bitmap: Bitmap,
        filePath: String,
        guideLines: List<GuideLine>,
        canvasWidth: Int,
        canvasHeight: Int
    ): ReferencePipelineResult {
        return try {
            val (hPixels, vPixels) = mapLinesToImageCoords(
                guideLines, canvasWidth, canvasHeight, bitmap.width, bitmap.height
            )
            val cells = gridProcessor.extract(bitmap, hPixels, vPixels)
            if (cells.isEmpty()) return ReferencePipelineResult.NoCells

            gridCellStore.save(filePath, cells)
            val embeddings = gridCellEmbedder.embed(cells)
            val existing = referenceDataStore.load(filePath)
            referenceDataStore.save(
                filePath,
                ReferencePhotoData(
                    guideLines = guideLines,
                    embeddings = embeddings.mapValues { it.value.toList() },
                    descriptions = existing.descriptions
                )
            )
            ReferencePipelineResult.Done(cells.size)
        } catch (e: Exception) {
            ReferencePipelineResult.Error(e.message ?: "Unknown error")
        }
    }
}
