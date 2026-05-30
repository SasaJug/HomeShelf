package com.jugurdzija.homeshelf.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import com.jugurdzija.homeshelf.di.DiConstants
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    @Named(DiConstants.NAMED_STORAGE_ROOT)
    fun provideStorageRoot(@ApplicationContext context: Context): File = context.filesDir
}
