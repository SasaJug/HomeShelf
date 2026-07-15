package com.jugurdzija.homeshelf.di

import com.jugurdzija.homeshelf.data.GridCellStore
import com.jugurdzija.homeshelf.data.GridCellStoreImpl
import com.jugurdzija.homeshelf.data.PendingCaptureStore
import com.jugurdzija.homeshelf.data.PendingCaptureStoreImpl
import com.jugurdzija.homeshelf.data.StorageRepository
import com.jugurdzija.homeshelf.data.StorageRepositoryImpl
import com.jugurdzija.homeshelf.usecase.ComparisonPipeline
import com.jugurdzija.homeshelf.usecase.ComparisonPipelineImpl
import com.jugurdzija.homeshelf.usecase.StorageSavePipeline
import com.jugurdzija.homeshelf.usecase.StorageSavePipelineImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindGridCellStore(impl: GridCellStoreImpl): GridCellStore

    @Binds
    @Singleton
    abstract fun bindComparisonPipeline(impl: ComparisonPipelineImpl): ComparisonPipeline

    @Binds
    @Singleton
    abstract fun bindStorageStore(impl: StorageRepositoryImpl): StorageRepository

    @Binds
    @Singleton
    abstract fun bindStorageSavePipeline(impl: StorageSavePipelineImpl): StorageSavePipeline

    @Binds
    @Singleton
    abstract fun bindPendingCaptureStore(impl: PendingCaptureStoreImpl): PendingCaptureStore
}
