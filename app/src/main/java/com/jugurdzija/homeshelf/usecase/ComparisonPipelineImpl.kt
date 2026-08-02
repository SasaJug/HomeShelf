package com.jugurdzija.homeshelf.usecase

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.data.StorageRepository
import com.jugurdzija.homeshelf.embedding.GridCellEmbedder
import com.jugurdzija.homeshelf.homography.GridProcessor
import com.jugurdzija.homeshelf.homography.HomographyProcessor
import com.jugurdzija.homeshelf.util.cosineSimilarity
import com.jugurdzija.homeshelf.util.mapLinesToImageCoords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComparisonPipelineImpl @Inject constructor(
    private val storageRepository: StorageRepository,
    private val gridProcessor: GridProcessor,
    private val gridCellEmbedder: GridCellEmbedder
) : ComparisonPipeline {

    override suspend fun run(capturedBitmap: Bitmap, storageId: String): ComparisonResult {
        val data = storageRepository.loadLatestData(storageId)
        if (data.guideLines.isEmpty()) return ComparisonResult.NoGuideLines
        if (data.embeddings.isEmpty()) return ComparisonResult.NoEmbeddings

        val referenceBitmap = storageRepository.decodeLatestBitmap(storageId)
            ?: return ComparisonResult.AlignmentFailed

        val aligned = withContext(Dispatchers.Default) {
            HomographyProcessor.align(capturedBitmap, referenceBitmap)
        } ?: return ComparisonResult.AlignmentFailed

        val (hPixels, vPixels) = mapLinesToImageCoords(
            data.guideLines, aligned.width, aligned.height, aligned.width, aligned.height
        )
        val cells = gridProcessor.extract(aligned, hPixels, vPixels)
        if (cells.isEmpty()) return ComparisonResult.NoCells
        val referenceCells = gridProcessor.extract(referenceBitmap, hPixels, vPixels)

        val refEmbeddings = data.embeddings.mapValues { it.value.toFloatArray() }
        val embeddings = gridCellEmbedder.embed(cells)
        val similarities = embeddings.mapValues { (name, vec) ->
            val refVec = refEmbeddings[name]
            if (refVec != null) cosineSimilarity(vec, refVec) else 0f
        }
        return ComparisonResult.Success(aligned, data.guideLines, similarities, referenceCells, cells, data.markedItems)
    }
}
