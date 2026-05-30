package com.jugurdzija.homeshelf.di

import com.jugurdzija.homeshelf.data.CaptureSettingsStore
import com.jugurdzija.homeshelf.data.CaptureSettingsStoreImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CaptureSettingsModule {

    @Binds
    @Singleton
    abstract fun bindCaptureSettingsStore(impl: CaptureSettingsStoreImpl): CaptureSettingsStore
}
