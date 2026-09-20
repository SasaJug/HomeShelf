package com.jugurdzija.homeshelf.domain.usecases.getstoragereference

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.domain.model.ReferencePhotoData

interface GetStorageReferenceUseCase {
    suspend fun getBitmap(storageId: String): Bitmap?
    suspend fun getThumbnail(storageId: String): Bitmap?
    suspend fun getData(storageId: String): ReferencePhotoData
}
