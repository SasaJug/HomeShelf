package com.jugurdzija.homeshelf.data

import kotlinx.serialization.Serializable

@Serializable
data class BoundingBox(val x: Float, val y: Float, val width: Float, val height: Float)
