package com.jugurdzija.homeshelf.data

interface GridCellStore {
    suspend fun save(imageFilePath: String, cells: List<GridCell>)
}
