package com.jugurdzija.homeshelf.embedding

import com.jugurdzija.homeshelf.data.ReferenceItem

data class ReferenceMatch(
    val item: ReferenceItem,
    val similarity: Double,
    val inferenceMs: Long
)
