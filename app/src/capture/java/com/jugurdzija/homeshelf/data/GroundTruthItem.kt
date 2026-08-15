package com.jugurdzija.homeshelf.data

import com.jugurdzija.homeshelf.llm.ItemChange

data class GroundTruthItem(
    val itemId: String? = null,
    val name: String,
    val changeType: ItemChange,
    val cellName: String? = null
)
