package com.jugurdzija.homeshelf.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import com.jugurdzija.homeshelf.di.DiConstants
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ReferenceImageStoreImpl @Inject constructor(
    @Named(DiConstants.NAMED_STORAGE_ROOT) private val storageRoot: File
) : ReferenceImageStore {

    private companion object {
        const val DIR_REFERENCES = "references"
        const val FILE_EXTENSION = "jpg"
        const val FILENAME_PREFIX = "ref_"
        const val LABEL_PREFIX = "Reference "
    }

    private val filenamePattern = Regex("""^${FILENAME_PREFIX}(\d+)_(.+)$""")

    private fun referencesDir(): File =
        File(storageRoot, DIR_REFERENCES).apply { mkdirs() }

    override suspend fun loadAll(): List<ReferenceItem> = withContext(Dispatchers.IO) {
        referencesDir()
            .listFiles { f -> f.extension == FILE_EXTENSION && f.nameWithoutExtension.matches(filenamePattern) }
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
            item.label.removePrefix(LABEL_PREFIX).toIntOrNull()
        }.maxOrNull() ?: 0
        val label = "$LABEL_PREFIX${maxNum + 1}"
        val slug = label.replace(' ', '_')
        val millis = System.currentTimeMillis()
        val file = File(referencesDir(), "${FILENAME_PREFIX}${millis}_${slug}.${FILE_EXTENSION}")
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
            val bitmap = BitmapFactory.decodeFile(
                item.file.absolutePath,
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
            ) ?: return@withContext null
            val orientation = ExifInterface(item.file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees == 0f) bitmap
            else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees) }, true)
        }
}
