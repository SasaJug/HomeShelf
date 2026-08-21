package com.jugurdzija.homeshelf.data

import java.io.File

data class GoldenItem(
    val name: String,
    val storageId: String? = null,
    val referenceLabel: String,
    val referenceFilePath: String = "",
    val timestamp: String,
    val dir: File,
    val groundTruth: List<GroundTruthItem> = emptyList()
)
