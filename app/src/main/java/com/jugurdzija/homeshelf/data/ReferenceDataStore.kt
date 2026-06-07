package com.jugurdzija.homeshelf.data

interface ReferenceDataStore {
    suspend fun save(filePath: String, data: ReferencePhotoData)
    suspend fun load(filePath: String): ReferencePhotoData
}
