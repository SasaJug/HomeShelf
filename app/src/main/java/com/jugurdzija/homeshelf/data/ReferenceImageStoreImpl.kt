package com.jugurdzija.homeshelf.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferenceImageStoreImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ReferenceImageStore {

    private val filenamePattern = Regex("""^ref_(\d+)_(.+)$""")

    private fun referencesDir(): File =
        File(context.filesDir, "references").apply { mkdirs() }

    private fun migrateIfNeeded() {
        val legacy = File(context.filesDir, "reference/reference.jpg")
        if (!legacy.exists()) return
        val dest = File(referencesDir(), "ref_${System.currentTimeMillis()}_Reference_1.jpg")
        legacy.copyTo(dest, overwrite = true)
        legacy.delete()
        legacy.parentFile?.delete()
    }

    override suspend fun loadAll(): List<ReferenceItem> = withContext(Dispatchers.IO) {
        migrateIfNeeded()
        referencesDir()
            .listFiles { f -> f.extension == "jpg" && f.nameWithoutExtension.matches(filenamePattern) }
            ?.mapNotNull { file ->
                val match = filenamePattern.matchEntire(file.nameWithoutExtension) ?: return@mapNotNull null
                val epoch = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
                val label = match.groupValues[2].replace('_', ' ')
                ReferenceItem(id = file.nameWithoutExtension, label = label, file = file)
            }
            ?.sortedBy { item ->
                filenamePattern.matchEntire(item.id)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            }
            ?: emptyList()
    }

    override suspend fun saveReference(bitmap: Bitmap): ReferenceItem = withContext(Dispatchers.IO) {
        val existing = loadAll()
        val maxNum = existing.mapNotNull { item ->
            item.label.removePrefix("Reference ").toIntOrNull()
        }.maxOrNull() ?: 0
        val label = "Reference ${maxNum + 1}"
        val slug = label.replace(' ', '_')
        val millis = System.currentTimeMillis()
        val file = File(referencesDir(), "ref_${millis}_${slug}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        ReferenceItem(id = file.nameWithoutExtension, label = label, file = file)
    }

    override suspend fun delete(id: String): Boolean = withContext(Dispatchers.IO) {
        referencesDir()
            .listFiles { f -> f.nameWithoutExtension == id }
            ?.firstOrNull()
            ?.delete()
            ?: false
    }

    override suspend fun decodeBitmap(item: ReferenceItem, sampleSize: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            if (!item.file.exists()) return@withContext null
            BitmapFactory.decodeFile(
                item.file.absolutePath,
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
            )
        }
}
