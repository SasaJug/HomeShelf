package com.jugurdzija.homeshelf.data

interface GridCellEmbeddingStore {
    suspend fun save(imageFilePath: String, embeddings: Map<String, FloatArray>)
    suspend fun load(imageFilePath: String): Map<String, FloatArray>
}
