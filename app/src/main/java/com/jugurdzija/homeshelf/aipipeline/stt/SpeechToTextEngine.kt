package com.jugurdzija.homeshelf.aipipeline.stt

interface SpeechToTextEngine {
    suspend fun transcribe(samples: FloatArray): Result<String>
}
