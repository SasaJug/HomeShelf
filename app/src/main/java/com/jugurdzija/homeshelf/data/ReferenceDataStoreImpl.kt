package com.jugurdzija.homeshelf.data

import com.jugurdzija.homeshelf.di.DiConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ReferenceDataStoreImpl @Inject constructor(
    @Named(DiConstants.NAMED_STORAGE_ROOT) private val storageRoot: File
) : ReferenceDataStore {

    private fun dataFile(filePath: String): File {
        val name = File(filePath).nameWithoutExtension
        return File(storageRoot, "grids/$name").apply { mkdirs() }.let {
            File(it, "data.json")
        }
    }

    override suspend fun save(filePath: String, data: ReferencePhotoData) = withContext(Dispatchers.IO) {
        val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
        dataFile(filePath).writeText(json.encodeToString(ReferencePhotoData.serializer(), data))
    }

    override suspend fun load(filePath: String): ReferencePhotoData = withContext(Dispatchers.IO) {
        val json = Json { ignoreUnknownKeys = true }
        val file = dataFile(filePath)
        if (!file.exists()) return@withContext ReferencePhotoData()
        json.decodeFromString(ReferencePhotoData.serializer(), file.readText())
    }
}
