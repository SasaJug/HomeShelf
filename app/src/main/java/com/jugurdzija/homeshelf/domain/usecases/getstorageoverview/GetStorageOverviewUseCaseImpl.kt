package com.jugurdzija.homeshelf.domain.usecases.getstorageoverview

import com.jugurdzija.homeshelf.data.storage.StorageRepository
import com.jugurdzija.homeshelf.domain.model.ReferencePhotoData
import com.jugurdzija.homeshelf.domain.model.StorageCompleteness
import com.jugurdzija.homeshelf.domain.model.StorageListEntry
import javax.inject.Inject

class GetStorageOverviewUseCaseImpl @Inject constructor(
    private val storageRepository: StorageRepository
) : GetStorageOverviewUseCase {

    override suspend fun getOverview(): List<StorageListEntry> =
        storageRepository.loadAllStorages().map { storage ->
            StorageListEntry(storage, storageRepository.loadStorageReferenceData(storage.id).calculateCompleteness())
        }

    private fun ReferencePhotoData.calculateCompleteness(): StorageCompleteness = when {
        markedItems.isEmpty() -> StorageCompleteness.NO_ITEMS
        guideLines.isEmpty() -> StorageCompleteness.NO_GRID
        else -> StorageCompleteness.COMPLETE
    }
}
