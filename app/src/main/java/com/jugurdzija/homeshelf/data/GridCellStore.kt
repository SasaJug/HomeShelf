package com.jugurdzija.homeshelf.data

import com.jugurdzija.homeshelf.domain.model.GridCell

interface GridCellStore {
    suspend fun save(imageFilePath: String, cells: List<GridCell>)
}
