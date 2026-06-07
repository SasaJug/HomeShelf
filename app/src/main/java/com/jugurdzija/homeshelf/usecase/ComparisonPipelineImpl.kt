package com.jugurdzija.homeshelf.usecase

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.data.GridCellEmbeddingStore
import com.jugurdzija.homeshelf.data.GuideLineStore
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
    private val guideLineStore: GuideLineStore,
    private val gridCellEmbeddingStore: GridCellEmbeddingStore,
    private val gridProcessor: GridProcessor,
    private val gridCellEmbedder: GridCellEmbedder
) : ComparisonPipeline {

    override suspend fun run(
        capturedBitmap: Bitmap,
        referenceBitmap: Bitmap,
        referenceFilePath: String
    ): ComparisonResult {
        val guideLines = guideLineStore.load(referenceFilePath)
        if (guideLines.isEmpty()) return ComparisonResult.NoGuideLines

        val refEmbeddings = gridCellEmbeddingStore.load(referenceFilePath)
        if (refEmbeddings.isEmpty()) return ComparisonResult.NoEmbeddings

        val aligned = withContext(Dispatchers.Default) {
            HomographyProcessor.align(capturedBitmap, referenceBitmap)
        } ?: return ComparisonResult.AlignmentFailed

        val (hPixels, vPixels) = mapLinesToImageCoords(
            guideLines, aligned.width, aligned.height, aligned.width, aligned.height
        )
        val cells = gridProcessor.extract(aligned, hPixels, vPixels)
        if (cells.isEmpty()) return ComparisonResult.NoCells

        val embeddings = gridCellEmbedder.embed(cells)
        val similarities = embeddings.mapValues { (name, vec) ->
            val refVec = refEmbeddings[name]
            if (refVec != null) cosineSimilarity(vec, refVec) else 0f
        }
        return ComparisonResult.Success(aligned, guideLines, similarities)
    }
}
