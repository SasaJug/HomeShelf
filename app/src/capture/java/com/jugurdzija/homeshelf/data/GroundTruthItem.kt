package com.jugurdzija.homeshelf.data

import com.jugurdzija.homeshelf.domain.model.BoundingBox
import com.jugurdzija.homeshelf.aipipeline.llm.ItemChange

data class GroundTruthItem(
    val itemId: String? = null,
    val name: String,
    val changeType: ItemChange,
    val cellName: String? = null,
     val box: BoundingBox? = null
)
