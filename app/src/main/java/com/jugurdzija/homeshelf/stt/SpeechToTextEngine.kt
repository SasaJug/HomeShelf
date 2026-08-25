package com.jugurdzija.homeshelf.stt

interface SpeechToTextEngine {
    suspend fun transcribe(samples: FloatArray): Result<String>
}
