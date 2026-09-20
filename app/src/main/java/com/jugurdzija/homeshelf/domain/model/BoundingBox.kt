package com.jugurdzija.homeshelf.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BoundingBox(val x: Float, val y: Float, val width: Float, val height: Float)
