package com.jugurdzija.homeshelf.data

interface GuideLineStore {
    suspend fun save(imageFilePath: String, lines: List<GuideLine>)
    suspend fun load(imageFilePath: String): List<GuideLine>
}
