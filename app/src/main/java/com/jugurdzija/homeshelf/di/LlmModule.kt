package com.jugurdzija.homeshelf.di

import com.jugurdzija.homeshelf.llm.ShelfDiffAnalyzer
import com.jugurdzija.homeshelf.llm.ShelfDiffAnalyzerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LlmModule {

    @Binds
    @Singleton
    abstract fun bindShelfDiffAnalyzer(impl: ShelfDiffAnalyzerImpl): ShelfDiffAnalyzer
}
