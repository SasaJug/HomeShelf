package com.jugurdzija.homeshelf.ui.golden

import androidx.compose.ui.graphics.Color
import com.jugurdzija.homeshelf.llm.ItemChange

internal val ItemChange.symbol: String
    get() = when (this) {
        ItemChange.UNCHANGED -> "○"
        ItemChange.REMOVED -> "−"
        ItemChange.REPLACED -> "≠"
        ItemChange.ADDED -> "+"
        ItemChange.PARTIALLY_CONSUMED -> "◐−"
        ItemChange.FULLY_CONSUMED -> "○−"
        ItemChange.PARTIALLY_FILLED -> "◐+"
        ItemChange.FULLY_FILLED -> "●+"
        ItemChange.UNKNOWN -> "?"
    }

internal val ItemChange.label: String
    get() = when (this) {
        ItemChange.UNCHANGED -> "Unchanged"
        ItemChange.REMOVED -> "Removed"
        ItemChange.REPLACED -> "Replaced"
        ItemChange.ADDED -> "Added"
        ItemChange.PARTIALLY_CONSUMED -> "Partially consumed"
        ItemChange.FULLY_CONSUMED -> "Fully consumed"
        ItemChange.PARTIALLY_FILLED -> "Partially filled"
        ItemChange.FULLY_FILLED -> "Fully filled"
        ItemChange.UNKNOWN -> "Unknown"
    }

internal val ItemChange.chipColor: Color
    get() = when (this) {
        ItemChange.UNCHANGED -> Color(0xFF388E3C)
        ItemChange.REMOVED -> Color(0xFFC62828)
        ItemChange.REPLACED -> Color(0xFFE65100)
        ItemChange.ADDED -> Color(0xFF1565C0)
        ItemChange.PARTIALLY_CONSUMED -> Color(0xFFEF6C00)
        ItemChange.FULLY_CONSUMED -> Color(0xFFB71C1C)
        ItemChange.PARTIALLY_FILLED -> Color(0xFF00838F)
        ItemChange.FULLY_FILLED -> Color(0xFF2E7D32)
        ItemChange.UNKNOWN -> Color(0xFF616161)
    }

internal val FillStates: List<ItemChange> = listOf(
    ItemChange.PARTIALLY_CONSUMED,
    ItemChange.FULLY_CONSUMED,
    ItemChange.PARTIALLY_FILLED,
    ItemChange.FULLY_FILLED
)
