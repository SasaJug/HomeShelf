package com.jugurdzija.homeshelf.aipipeline.embedding

import com.jugurdzija.homeshelf.domain.model.StorageItem

data class ReferenceMatch(
    val item: StorageItem,
    val similarity: Double,
    val inferenceMs: Long
)
