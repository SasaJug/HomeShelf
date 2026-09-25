package com.jugurdzija.homeshelf.aipipeline.embedding

import com.jugurdzija.homeshelf.domain.model.GridCell

interface GridCellEmbedder {
    suspend fun embed(cells: List<GridCell>): Map<String, FloatArray>
}
