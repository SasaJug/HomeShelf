package com.jugurdzija.homeshelf.data

import kotlinx.serialization.Serializable

@Serializable
data class ReferencePhotoData(
    val guideLines: List<GuideLine> = emptyList(),
    val embeddings: Map<String, List<Float>> = emptyMap(),
    val descriptions: Map<String, CellDescription> = emptyMap()
)
