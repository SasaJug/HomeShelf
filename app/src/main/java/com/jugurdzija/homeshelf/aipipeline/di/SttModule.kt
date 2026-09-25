package com.jugurdzija.homeshelf.aipipeline.di

import com.jugurdzija.homeshelf.aipipeline.stt.SpeechToTextEngine
import com.jugurdzija.homeshelf.aipipeline.stt.VoskSpeechToTextEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SttModule {

    @Binds
    @Singleton
    abstract fun bindSpeechToTextEngine(impl: VoskSpeechToTextEngine): SpeechToTextEngine
}
