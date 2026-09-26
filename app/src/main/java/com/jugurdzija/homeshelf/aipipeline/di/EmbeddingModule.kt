package com.jugurdzija.homeshelf.aipipeline.di

import com.jugurdzija.homeshelf.aipipeline.embedding.EmbedderOwner
import com.jugurdzija.homeshelf.aipipeline.embedding.EmbedderOwnerImpl
import com.jugurdzija.homeshelf.aipipeline.embedding.GridCellEmbedder
import com.jugurdzija.homeshelf.aipipeline.embedding.GridCellEmbedderImpl
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
