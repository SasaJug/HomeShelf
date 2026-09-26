package com.jugurdzija.homeshelf.di

import com.jugurdzija.homeshelf.data.onboarding.OnboardingRepository
import com.jugurdzija.homeshelf.data.onboarding.OnboardingRepositoryImpl
import com.jugurdzija.homeshelf.data.pendingcapture.PendingCaptureRepository
import com.jugurdzija.homeshelf.data.pendingcapture.PendingCaptureRepositoryImpl
import com.jugurdzija.homeshelf.data.shoppinglist.ShoppingListRepository
import com.jugurdzija.homeshelf.data.shoppinglist.ShoppingListRepositoryImpl
import com.jugurdzija.homeshelf.data.storage.StorageRepository
import com.jugurdzija.homeshelf.data.storage.StorageRepositoryImpl
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
    abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository

    @Binds
    @Singleton
    abstract fun bindPendingCaptureRepository(impl: PendingCaptureRepositoryImpl): PendingCaptureRepository

    @Binds
    @Singleton
    abstract fun bindShoppingListRepository(impl: ShoppingListRepositoryImpl): ShoppingListRepository

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(impl: OnboardingRepositoryImpl): OnboardingRepository
}
