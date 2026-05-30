package com.jugurdzija.homeshelf.di

import com.jugurdzija.homeshelf.data.GoldenStore
import com.jugurdzija.homeshelf.data.GoldenStoreImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CaptureDataModule {

    @Binds
    @Singleton
    abstract fun bindGoldenStore(impl: GoldenStoreImpl): GoldenStore
}
