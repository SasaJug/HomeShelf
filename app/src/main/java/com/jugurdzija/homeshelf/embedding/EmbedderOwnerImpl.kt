package com.jugurdzija.homeshelf.embedding

import android.content.Context
import android.graphics.Bitmap
import com.jugurdzija.homeshelf.data.StorageItem
import com.jugurdzija.homeshelf.util.ImageEmbedderHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmbedderOwnerImpl @Inject constructor(
    @ApplicationContext appContext: Context
) : EmbedderOwner, ImageEmbedderHelper.EmbedderListener {

    private val helper = ImageEmbedderHelper(
        context = appContext,
        currentDelegate = ImageEmbedderHelper.DELEGATE_CPU,
        currentModel = ImageEmbedderHelper.MODEL_SMALL,
        listener = this
    )

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val errors: SharedFlow<String> = _errors

    override suspend fun embed(
        reference: Bitmap,
        candidate: Bitmap
    ): ImageEmbedderHelper.ResultBundle? = withContext(Dispatchers.Default) {
        try {
            helper.embed(reference, candidate)
        } catch (e: Exception) {
            _errors.tryEmit("Embedding error: ${e.message}")
            null
        }
    }

    override suspend fun embedAll(
        candidate: Bitmap,
        references: List<Pair<StorageItem, Bitmap>>,
        topN: Int
    ): List<ReferenceMatch> = withContext(Dispatchers.Default) {
        references.mapNotNull { (item, refBitmap) ->
            try {
                val result = helper.embed(refBitmap, candidate) ?: return@mapNotNull null
                ReferenceMatch(item = item, similarity = result.similarity, inferenceMs = result.inferenceTime)
            } catch (e: Exception) {
                _errors.tryEmit("Error embedding ${item.name}: ${e.message}")
                null
            }
        }
            .sortedByDescending { it.similarity }
            .take(topN)
    }

    override fun close() {
        helper.clearImageEmbedder()
    }

    override fun onError(error: String, errorCode: Int) {
        _errors.tryEmit(error)
    }
}
