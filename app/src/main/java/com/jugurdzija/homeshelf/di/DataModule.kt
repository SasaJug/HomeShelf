package com.jugurdzija.homeshelf.di

import com.jugurdzija.homeshelf.data.GridCellStore
import com.jugurdzija.homeshelf.data.GridCellStoreImpl
import com.jugurdzija.homeshelf.data.ReferenceDataStore
import com.jugurdzija.homeshelf.data.ReferenceDataStoreImpl
import com.jugurdzija.homeshelf.data.ReferenceImageStore
import com.jugurdzija.homeshelf.data.ReferenceImageStoreImpl
import com.jugurdzija.homeshelf.usecase.ComparisonPipeline
import com.jugurdzija.homeshelf.usecase.ComparisonPipelineImpl
import com.jugurdzija.homeshelf.usecase.ReferencePipeline
import com.jugurdzija.homeshelf.usecase.ReferencePipelineImpl
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
    abstract fun bindReferenceImageStore(impl: ReferenceImageStoreImpl): ReferenceImageStore

    @Binds
    @Singleton
    abstract fun bindGridCellStore(impl: GridCellStoreImpl): GridCellStore

    @Binds
    @Singleton
    abstract fun bindReferenceDataStore(impl: ReferenceDataStoreImpl): ReferenceDataStore

    @Binds
    @Singleton
    abstract fun bindReferencePipeline(impl: ReferencePipelineImpl): ReferencePipeline

    @Binds
    @Singleton
    abstract fun bindComparisonPipeline(impl: ComparisonPipelineImpl): ComparisonPipeline
}
