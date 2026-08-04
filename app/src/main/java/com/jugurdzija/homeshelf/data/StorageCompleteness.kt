package com.jugurdzija.homeshelf.data

enum class StorageCompleteness {
    COMPLETE, NO_ITEMS, NO_GRID
}

data class StorageListEntry(val item: StorageItem, val completeness: StorageCompleteness)

fun calculateCompleteness(data: ReferencePhotoData): StorageCompleteness = when {
    data.markedItems.isEmpty() -> StorageCompleteness.NO_ITEMS
    data.guideLines.isEmpty() -> StorageCompleteness.NO_GRID
    else -> StorageCompleteness.COMPLETE
}
