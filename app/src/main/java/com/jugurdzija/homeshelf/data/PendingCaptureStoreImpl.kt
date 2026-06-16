package com.jugurdzija.homeshelf.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.jugurdzija.homeshelf.di.DiConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class PendingCaptureStoreImpl @Inject constructor(
    @Named(DiConstants.NAMED_STORAGE_ROOT) private val storageRoot: File
) : PendingCaptureStore {

    private companion object {
        const val DIR_PENDING = "pending"
        const val FILE_CAPTURE = "capture.jpg"
    }

    private fun captureFile(): File =
        File(storageRoot, DIR_PENDING).apply { mkdirs() }.let { File(it, FILE_CAPTURE) }

    override suspend fun save(bitmap: Bitmap) = withContext(Dispatchers.IO) {
        FileOutputStream(captureFile()).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        Unit
    }

    override suspend fun load(): Bitmap? = withContext(Dispatchers.IO) {
        val file = captureFile()
        if (!file.exists()) return@withContext null
        BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        )
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        captureFile().delete()
        Unit
    }
}
