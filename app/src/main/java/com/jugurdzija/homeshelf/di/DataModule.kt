package com.jugurdzija.homeshelf.di

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
    abstract fun bindReferenceImageStore(
        impl: ReferenceImageStoreImpl
    ): ReferenceImageStore
}
