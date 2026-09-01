package com.jugurdzija.homeshelf.stt

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpSpeechToTextEngine @Inject constructor() : SpeechToTextEngine {

    override suspend fun transcribe(samples: FloatArray): Result<String> {
        return Result.failure(UnsupportedOperationException("Speech-to-text engine not configured"))
    }
}
