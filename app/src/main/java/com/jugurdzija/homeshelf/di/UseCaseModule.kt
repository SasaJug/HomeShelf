package com.jugurdzija.homeshelf.di

import com.jugurdzija.homeshelf.domain.usecases.getstorage.GetStorageUseCase
import com.jugurdzija.homeshelf.domain.usecases.getstorage.GetStorageUseCaseImpl
import com.jugurdzija.homeshelf.domain.usecases.getstorageoverview.GetStorageOverviewUseCase
import com.jugurdzija.homeshelf.domain.usecases.getstorageoverview.GetStorageOverviewUseCaseImpl
import com.jugurdzija.homeshelf.domain.usecases.getstoragereference.GetStorageReferenceUseCase
import com.jugurdzija.homeshelf.domain.usecases.getstoragereference.GetStorageReferenceUseCaseImpl
import com.jugurdzija.homeshelf.domain.usecases.introseen.IntroSeenUseCase
import com.jugurdzija.homeshelf.domain.usecases.introseen.IntroSeenUseCaseImpl
import com.jugurdzija.homeshelf.domain.usecases.managestorage.ManageStorageUseCase
import com.jugurdzija.homeshelf.domain.usecases.managestorage.ManageStorageUseCaseImpl
import com.jugurdzija.homeshelf.domain.usecases.pendingcapture.PendingCaptureUseCase
import com.jugurdzija.homeshelf.domain.usecases.pendingcapture.PendingCaptureUseCaseImpl
import com.jugurdzija.homeshelf.domain.usecases.shoppinglist.ShoppingListUseCase
import com.jugurdzija.homeshelf.domain.usecases.shoppinglist.ShoppingListUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class UseCaseModule {

    @Binds
    abstract fun bindGetStorageUseCase(impl: GetStorageUseCaseImpl): GetStorageUseCase

    @Binds
    abstract fun bindGetStorageOverviewUseCase(impl: GetStorageOverviewUseCaseImpl): GetStorageOverviewUseCase

    @Binds
    abstract fun bindGetStorageReferenceUseCase(impl: GetStorageReferenceUseCaseImpl): GetStorageReferenceUseCase

    @Binds
    abstract fun bindIntroSeenUseCase(impl: IntroSeenUseCaseImpl): IntroSeenUseCase

    @Binds
    abstract fun bindManageStorageUseCase(impl: ManageStorageUseCaseImpl): ManageStorageUseCase

    @Binds
    abstract fun bindPendingCaptureUseCase(impl: PendingCaptureUseCaseImpl): PendingCaptureUseCase

    @Binds
    abstract fun bindShoppingListUseCase(impl: ShoppingListUseCaseImpl): ShoppingListUseCase
}
