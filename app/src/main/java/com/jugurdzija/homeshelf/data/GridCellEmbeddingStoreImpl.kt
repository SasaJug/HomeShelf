package com.jugurdzija.homeshelf.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GridCellEmbeddingStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GridCellEmbeddingStore {

    private fun embeddingsFile(imageFilePath: String): File {
        val name = File(imageFilePath).nameWithoutExtension
        val dir = File(context.filesDir, "grids/$name").apply { mkdirs() }
        return File(dir, "embeddings.csv")
    }

    override suspend fun save(imageFilePath: String, embeddings: Map<String, FloatArray>) = withContext(Dispatchers.IO) {
        embeddingsFile(imageFilePath).bufferedWriter().use { writer ->
            embeddings.forEach { (name, vector) ->
                writer.write(name)
                vector.forEach { v -> writer.write(",${v}") }
                writer.newLine()
            }
        }
    }

    override suspend fun load(imageFilePath: String): Map<String, FloatArray> = withContext(Dispatchers.IO) {
        val file = embeddingsFile(imageFilePath)
        if (!file.exists()) return@withContext emptyMap()
        file.bufferedReader().lineSequence()
            .filter { it.isNotBlank() }
            .associate { line ->
                val parts = line.split(",")
                val name = parts[0]
                val vector = FloatArray(parts.size - 1) { i -> parts[i + 1].toFloat() }
                name to vector
            }
    }
}
