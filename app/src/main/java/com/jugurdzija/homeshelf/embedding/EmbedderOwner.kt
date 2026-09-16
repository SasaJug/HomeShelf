package com.jugurdzija.homeshelf.embedding

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.domain.model.StorageItem
import com.jugurdzija.homeshelf.util.ImageEmbedderHelper
import kotlinx.coroutines.flow.SharedFlow

interface EmbedderOwner {
    val errors: SharedFlow<String>
    suspend fun embed(reference: Bitmap, candidate: Bitmap): ImageEmbedderHelper.ResultBundle?
    suspend fun embedAll(
        candidate: Bitmap,
        references: List<Pair<StorageItem, Bitmap>>,
        topN: Int = 3
    ): List<ReferenceMatch>
    fun close()
}
