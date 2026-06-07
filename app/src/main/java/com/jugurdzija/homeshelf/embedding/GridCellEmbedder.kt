package com.jugurdzija.homeshelf.embedding

import com.jugurdzija.homeshelf.data.GridCell

interface GridCellEmbedder {
    suspend fun embed(cells: List<GridCell>): Map<String, FloatArray>
}
