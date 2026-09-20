package com.jugurdzija.homeshelf.data.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.jugurdzija.homeshelf.di.DiConstants
import com.jugurdzija.homeshelf.domain.model.MarkedItem
import com.jugurdzija.homeshelf.domain.model.ReferencePhotoData
import com.jugurdzija.homeshelf.domain.model.StorageItem
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
class StorageRepositoryImpl @Inject constructor(
    @Named(DiConstants.NAMED_STORAGE_ROOT) private val storageRoot: File
) : StorageRepository {

    private companion object {
        const val DIR_STORAGES = "storages"
        const val DIR_REFERENCE = "latest"
        const val FILE_META = "meta.json"
        const val FILE_PHOTO = "photo.jpg"
        const val FILE_DATA = "data.json"
        const val SAMPLE_SIZE_FULL = 1
        const val SAMPLE_SIZE_THUMBNAIL = 4
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private fun storagesDir(): File =
        File(storageRoot, DIR_STORAGES).apply { mkdirs() }

    private fun storageDir(id: String): File = File(storagesDir(), id)

    private fun metaFile(id: String): File = File(storageDir(id), FILE_META)

    private fun referenceDir(id: String): File = File(storageDir(id), DIR_REFERENCE)

    private fun readMeta(file: File): StorageItem? {
        if (!file.exists()) return null
        return json.decodeFromString(StorageItem.serializer(), file.readText())
    }

    private fun writeMeta(item: StorageItem) {
        metaFile(item.id).writeText(json.encodeToString(StorageItem.serializer(), item))
    }

    private suspend fun getAndDecodeReferencePhoto(id: String, sampleSize: Int): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(referenceDir(id), FILE_PHOTO)
        if (!file.exists()) return@withContext null
        BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        )
    }

    override suspend fun loadAllStorages(): List<StorageItem> = withContext(Dispatchers.IO) {
        storagesDir()
            .listFiles { f -> f.isDirectory }
            ?.mapNotNull { dir -> readMeta(File(dir, FILE_META)) }
            ?.sortedByDescending { it.updatedAt }
            ?: emptyList()
    }

    override suspend fun getStorageReferenceBitmap(id: String): Bitmap? =
        getAndDecodeReferencePhoto(id, SAMPLE_SIZE_FULL)

    override suspend fun getStorageReferenceThumbnail(id: String): Bitmap? =
        getAndDecodeReferencePhoto(id, SAMPLE_SIZE_THUMBNAIL)


    override suspend fun loadStorageReferenceData(id: String): ReferencePhotoData = withContext(Dispatchers.IO) {
        val file = File(referenceDir(id), FILE_DATA)
        if (!file.exists()) return@withContext ReferencePhotoData()
        json.decodeFromString(ReferencePhotoData.serializer(), file.readText())
    }

    override suspend fun createStorage(name: String): StorageItem = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        referenceDir(id).mkdirs()
        val now = System.currentTimeMillis()
        val item = StorageItem(id = id, name = name, createdAt = now, updatedAt = now)
        writeMeta(item)
        item
    }

    override suspend fun saveStorageReference(
        id: String,
        bitmap: Bitmap,
        data: ReferencePhotoData
    ) = withContext(Dispatchers.IO) {
        val reference = referenceDir(id).apply { mkdirs() }

        FileOutputStream(File(reference, FILE_PHOTO)).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        File(reference, FILE_DATA).writeText(json.encodeToString(ReferencePhotoData.serializer(), data))

        val meta = readMeta(metaFile(id))
        if (meta != null) {
            writeMeta(meta.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun saveStorageReferenceMarkedItems(id: String, items: List<MarkedItem>) = withContext(Dispatchers.IO) {
        val current = loadStorageReferenceData(id)
        val updated = current.copy(markedItems = items)
        File(referenceDir(id), FILE_DATA).writeText(json.encodeToString(ReferencePhotoData.serializer(), updated))
        val meta = readMeta(metaFile(id))
        if (meta != null) {
            writeMeta(meta.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun renameStorage(id: String, name: String) = withContext(Dispatchers.IO) {
        val meta = readMeta(metaFile(id))
        if (meta != null) {
            writeMeta(meta.copy(name = name, updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun deleteStorage(id: String) = withContext(Dispatchers.IO) {
        storageDir(id).deleteRecursively()
        Unit
    }
}
