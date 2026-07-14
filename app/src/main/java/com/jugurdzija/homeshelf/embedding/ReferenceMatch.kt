package com.jugurdzija.homeshelf.embedding

import com.jugurdzija.homeshelf.data.StorageItem

data class ReferenceMatch(
    val item: StorageItem,
    val similarity: Double,
    val inferenceMs: Long
)
