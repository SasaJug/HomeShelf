package com.jugurdzija.homeshelf.aipipeline.di

import com.jugurdzija.homeshelf.aipipeline.llm.GridLineGenerator
import com.jugurdzija.homeshelf.aipipeline.llm.GridLineGeneratorImpl
import com.jugurdzija.homeshelf.aipipeline.llm.ItemDetector
import com.jugurdzija.homeshelf.aipipeline.llm.ItemDetectorImpl
import com.jugurdzija.homeshelf.aipipeline.llm.ShelfDiffAnalyzer
import com.jugurdzija.homeshelf.aipipeline.llm.ShelfDiffAnalyzerImpl
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

    @Binds
    @Singleton
    abstract fun bindGridLineGenerator(impl: GridLineGeneratorImpl): GridLineGenerator

    @Binds
    @Singleton
    abstract fun bindItemDetector(impl: ItemDetectorImpl): ItemDetector
}
