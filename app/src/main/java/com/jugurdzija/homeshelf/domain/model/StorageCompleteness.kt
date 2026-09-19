package com.jugurdzija.homeshelf.domain.model

enum class StorageCompleteness {
    COMPLETE, NO_ITEMS, NO_GRID
}

data class StorageListEntry(val item: StorageItem, val completeness: StorageCompleteness)
