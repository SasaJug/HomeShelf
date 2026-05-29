package com.jugurdzija.homeshelf.data

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class GridCellStoreImpl @Inject constructor(
    @Named("storageRoot") private val storageRoot: File
) : GridCellStore {

    private fun cellsDir(imageFilePath: String): File {
        val name = File(imageFilePath).nameWithoutExtension
        return File(storageRoot, "grids/$name").apply { mkdirs() }
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
