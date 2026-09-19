package com.jugurdzija.homeshelf.domain.usecases.getstoragereference

import android.graphics.Bitmap
import com.jugurdzija.homeshelf.data.storage.StorageRepository
import com.jugurdzija.homeshelf.domain.model.ReferencePhotoData
import javax.inject.Inject

class GetStorageReferenceUseCaseImpl @Inject constructor(
    private val storageRepository: StorageRepository
) : GetStorageReferenceUseCase {

    override suspend fun getBitmap(storageId: String): Bitmap? =
        storageRepository.getStorageReferenceBitmap(storageId)

    override suspend fun getThumbnail(storageId: String): Bitmap? =
        storageRepository.getStorageReferenceThumbnail(storageId)

    override suspend fun getData(storageId: String): ReferencePhotoData =
        storageRepository.loadStorageReferenceData(storageId)
}
