package com.jugurdzija.homeshelf.aipipeline.llm

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.domain.model.BoundingBox

data class DetectedItem(
    val name: String,
    val box: BoundingBox,
    val isTransparentContainer: Boolean
)

interface ItemDetector {
    suspend fun detect(bitmap: Bitmap): Result<List<DetectedItem>>
}
