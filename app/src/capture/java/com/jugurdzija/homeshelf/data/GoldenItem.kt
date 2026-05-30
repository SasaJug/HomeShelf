package com.jugurdzija.homeshelf.data

import java.io.File

data class GoldenItem(
    val name: String,
    val referenceLabel: String,
    val timestamp: String,
    val dir: File
)
