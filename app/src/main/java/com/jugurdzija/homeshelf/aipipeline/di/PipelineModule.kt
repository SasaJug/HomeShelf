package com.jugurdzija.homeshelf.aipipeline.di

import com.jugurdzija.homeshelf.aipipeline.pipeline.ComparisonPipeline
import com.jugurdzija.homeshelf.aipipeline.pipeline.ComparisonPipelineImpl
import com.jugurdzija.homeshelf.aipipeline.pipeline.StorageSavePipeline
import com.jugurdzija.homeshelf.aipipeline.pipeline.StorageSavePipelineImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PipelineModule {

    @Binds
    @Singleton
    abstract fun bindComparisonPipeline(impl: ComparisonPipelineImpl): ComparisonPipeline

    @Binds
    @Singleton
    abstract fun bindStorageSavePipeline(impl: StorageSavePipelineImpl): StorageSavePipeline
}
