package com.jugurdzija.homeshelf.di

import com.jugurdzija.homeshelf.embedding.EmbedderOwner
import com.jugurdzija.homeshelf.embedding.EmbedderOwnerImpl
import com.jugurdzija.homeshelf.embedding.GridCellEmbedder
import com.jugurdzija.homeshelf.embedding.GridCellEmbedderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EmbeddingModule {

    @Binds
    @Singleton
    abstract fun bindEmbedderOwner(impl: EmbedderOwnerImpl): EmbedderOwner

    @Binds
    @Singleton
    abstract fun bindGridCellEmbedder(impl: GridCellEmbedderImpl): GridCellEmbedder
}
