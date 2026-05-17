package com.jugurdzija.homeshelf.data

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GridCellStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GridCellStore {

    private fun cellsDir(imageFilePath: String): File {
        val name = File(imageFilePath).nameWithoutExtension
        return File(context.filesDir, "grids/$name").apply { mkdirs() }
    }

    override suspend fun save(imageFilePath: String, cells: List<GridCell>) = withContext(Dispatchers.IO) {
        val dir = cellsDir(imageFilePath)
        cells.forEach { cell ->
            FileOutputStream(File(dir, "${cell.name}.jpg")).use { out ->
                cell.bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
        }
    }
}
