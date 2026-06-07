package com.jugurdzija.homeshelf.embedding

import android.content.Context
import com.jugurdzija.homeshelf.data.GridCell
import com.jugurdzija.homeshelf.util.ImageEmbedderHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GridCellEmbedderImpl @Inject constructor(
    @ApplicationContext appContext: Context
) : GridCellEmbedder {

    private val helper = ImageEmbedderHelper(
        context = appContext,
        currentDelegate = ImageEmbedderHelper.DELEGATE_CPU,
        currentModel = ImageEmbedderHelper.MODEL_SMALL
    )

    override suspend fun embed(cells: List<GridCell>): Map<String, FloatArray> = withContext(Dispatchers.Default) {
        cells.mapNotNull { cell ->
            val vector = helper.embedSingle(cell.bitmap) ?: return@mapNotNull null
            cell.name to vector
        }.toMap()
    }
}
