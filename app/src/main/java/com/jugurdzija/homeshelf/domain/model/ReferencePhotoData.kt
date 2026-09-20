package com.jugurdzija.homeshelf.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ReferencePhotoData(
    val guideLines: List<GuideLine> = emptyList(),
    val embeddings: Map<String, List<Float>> = emptyMap(),
    val markedItems: List<MarkedItem> = emptyList()
)
