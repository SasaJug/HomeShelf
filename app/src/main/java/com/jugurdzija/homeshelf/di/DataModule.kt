package com.jugurdzija.homeshelf.di

import com.jugurdzija.homeshelf.data.GridCellEmbeddingStore
import com.jugurdzija.homeshelf.data.GridCellEmbeddingStoreImpl
import com.jugurdzija.homeshelf.data.GridCellStore
import com.jugurdzija.homeshelf.data.GridCellStoreImpl
import com.jugurdzija.homeshelf.data.GuideLineStore
import com.jugurdzija.homeshelf.data.GuideLineStoreImpl
import com.jugurdzija.homeshelf.data.ReferenceImageStore
import com.jugurdzija.homeshelf.data.ReferenceImageStoreImpl
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
    abstract fun bindGuideLineStore(impl: GuideLineStoreImpl): GuideLineStore

    @Binds
    @Singleton
    abstract fun bindGridCellStore(impl: GridCellStoreImpl): GridCellStore

    @Binds
    @Singleton
    abstract fun bindGridCellEmbeddingStore(impl: GridCellEmbeddingStoreImpl): GridCellEmbeddingStore
}
