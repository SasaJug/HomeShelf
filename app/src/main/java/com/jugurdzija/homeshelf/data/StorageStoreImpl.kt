package com.jugurdzija.homeshelf.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.jugurdzija.homeshelf.di.DiConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class StorageStoreImpl @Inject constructor(
    @Named(DiConstants.NAMED_STORAGE_ROOT) private val storageRoot: File
) : StorageStore {

    private companion object {
        const val DIR_STORAGES = "storages"
        const val DIR_LATEST = "latest"
        const val DIR_HISTORY = "history"
        const val DIR_CELLS = "cells"
        const val FILE_META = "meta.json"
        const val FILE_PHOTO = "photo.jpg"
        const val FILE_DATA = "data.json"
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private fun storagesDir(): File =
        File(storageRoot, DIR_STORAGES).apply { mkdirs() }

    private fun storageDir(id: String): File = File(storagesDir(), id)

    private fun metaFile(id: String): File = File(storageDir(id), FILE_META)

    private fun latestDir(id: String): File = File(storageDir(id), DIR_LATEST)

    private fun historyDir(id: String): File = File(storageDir(id), DIR_HISTORY)

    override suspend fun loadAll(): List<StorageItem> = withContext(Dispatchers.IO) {
        storagesDir()
            .listFiles { f -> f.isDirectory }
            ?.mapNotNull { dir -> readMeta(File(dir, FILE_META)) }
            ?.sortedByDescending { it.updatedAt }
            ?: emptyList()
    }

    private fun readMeta(file: File): StorageItem? {
        if (!file.exists()) return null
        return json.decodeFromString(StorageItem.serializer(), file.readText())
    }

    private fun writeMeta(item: StorageItem) {
        metaFile(item.id).writeText(json.encodeToString(StorageItem.serializer(), item))
    }

    override suspend fun decodeLatestBitmap(id: String, sampleSize: Int): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(latestDir(id), FILE_PHOTO)
        if (!file.exists()) return@withContext null
        BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        )
    }

    override suspend fun loadLatestData(id: String): ReferencePhotoData = withContext(Dispatchers.IO) {
        val file = File(latestDir(id), FILE_DATA)
        if (!file.exists()) return@withContext ReferencePhotoData()
        json.decodeFromString(ReferencePhotoData.serializer(), file.readText())
    }

    override suspend fun createNew(name: String): StorageItem = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        latestDir(id).mkdirs()
        historyDir(id).mkdirs()
        val now = System.currentTimeMillis()
        val item = StorageItem(id = id, name = name, createdAt = now, updatedAt = now)
        writeMeta(item)
        item
    }

    override suspend fun saveLatest(
        id: String,
        bitmap: Bitmap,
        data: ReferencePhotoData,
        cells: List<GridCell>
    ) = withContext(Dispatchers.IO) {
        val latest = latestDir(id)
        if (File(latest, FILE_PHOTO).exists()) {
            val target = File(historyDir(id), System.currentTimeMillis().toString())
            latest.renameTo(target)
            latest.mkdirs()
        }

        FileOutputStream(File(latest, FILE_PHOTO)).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        File(latest, FILE_DATA).writeText(json.encodeToString(ReferencePhotoData.serializer(), data))

        if (cells.isNotEmpty()) {
            val cellsDir = File(latest, DIR_CELLS).apply { mkdirs() }
            cells.forEach { cell ->
                FileOutputStream(File(cellsDir, "${cell.name}.jpg")).use { out ->
                    cell.bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
            }
        }

        val meta = readMeta(metaFile(id))
        if (meta != null) {
            writeMeta(meta.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun getLatestPhotoFile(id: String): File? = withContext(Dispatchers.IO) {
        val file = File(latestDir(id), FILE_PHOTO)
        if (file.exists()) file else null
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        storageDir(id).deleteRecursively()
        Unit
    }
}
