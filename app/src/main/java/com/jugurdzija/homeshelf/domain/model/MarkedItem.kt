package com.jugurdzija.homeshelf.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MarkedItem(
    val id: String,
    val name: String,
    val boundingBox: BoundingBox,
    val isTransparentContainer: Boolean = false
)
