package com.jugurdzija.homeshelf.domain.usecases.getstorageoverview

import com.jugurdzija.homeshelf.domain.model.StorageListEntry

interface GetStorageOverviewUseCase {
    suspend fun getOverview(): List<StorageListEntry>
}
