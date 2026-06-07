package com.jugurdzija.homeshelf.data

import kotlinx.serialization.Serializable

@Serializable
data class CellDescription(
    val location: String,
    val visualDescription: String,
    val isTransparentContainer: Boolean,
    val fillPercentage: Int?,
    val bestGuess: String
)
