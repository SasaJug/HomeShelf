package com.jugurdzija.homeshelf.usecase

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.ReferencePhotoData
import com.jugurdzija.homeshelf.data.StorageRepository
import com.jugurdzija.homeshelf.embedding.GridCellEmbedder
import com.jugurdzija.homeshelf.homography.GridProcessor
import com.jugurdzija.homeshelf.util.mapLinesToImageCoords
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageSavePipelineImpl @Inject constructor(
    private val storageRepository: StorageRepository,
    private val gridProcessor: GridProcessor,
    private val gridCellEmbedder: GridCellEmbedder
) : StorageSavePipeline {

    override suspend fun run(
        storageId: String?,
        name: String,
        bitmap: Bitmap,
        guideLines: List<GuideLine>,
        canvasWidth: Int,
        canvasHeight: Int
    ): StorageSaveResult {
        return try {
            val id = storageId ?: storageRepository.createNew(name).id
            val existingDescriptions = if (storageId != null) {
                storageRepository.loadLatestData(id).descriptions
            } else {
                emptyMap()
            }

            val (hPixels, vPixels) = mapLinesToImageCoords(
                guideLines, canvasWidth, canvasHeight, bitmap.width, bitmap.height
            )
            val cells = gridProcessor.extract(bitmap, hPixels, vPixels)
            val embeddings = gridCellEmbedder.embed(cells)

            storageRepository.saveLatest(
                id,
                bitmap,
                ReferencePhotoData(
                    guideLines = guideLines,
                    embeddings = embeddings.mapValues { it.value.toList() },
                    descriptions = existingDescriptions
                ),
                cells
            )
            StorageSaveResult.Done(id, cells.size)
        } catch (e: Exception) {
            StorageSaveResult.Error(e.message ?: "Unknown error")
        }
    }
}
