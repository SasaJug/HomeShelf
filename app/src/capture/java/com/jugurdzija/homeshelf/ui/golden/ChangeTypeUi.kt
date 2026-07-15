package com.jugurdzija.homeshelf.ui.golden

import androidx.compose.ui.graphics.Color
import com.jugurdzija.homeshelf.data.ChangeType

internal val ChangeType.symbol: String
    get() = when (this) {
        ChangeType.NO_CHANGE -> "○"
        ChangeType.ITEM_ADDED -> "+"
        ChangeType.ITEM_REMOVED -> "−"
        ChangeType.ITEM_REPLACED -> "≠"
    }

internal val ChangeType.label: String
    get() = when (this) {
        ChangeType.NO_CHANGE -> "No change"
        ChangeType.ITEM_ADDED -> "Item added"
        ChangeType.ITEM_REMOVED -> "Item removed"
        ChangeType.ITEM_REPLACED -> "Item replaced"
    }

internal val ChangeType.chipColor: Color
    get() = when (this) {
        ChangeType.NO_CHANGE -> Color(0xFF388E3C)
        ChangeType.ITEM_ADDED -> Color(0xFF1565C0)
        ChangeType.ITEM_REMOVED -> Color(0xFFC62828)
        ChangeType.ITEM_REPLACED -> Color(0xFFE65100)
    }
