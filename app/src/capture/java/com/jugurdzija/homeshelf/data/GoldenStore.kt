package com.jugurdzija.homeshelf.data

interface GoldenStore {
    suspend fun loadAll(): List<GoldenItem>
    suspend fun delete(name: String): Boolean
    suspend fun loadIntoHolder(name: String): Boolean
}
