package com.jugurdzija.homeshelf.stt

import android.content.Context
import com.whispercpp.whisper.WhisperContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val MODEL_ASSET_PATH = "models/ggml-tiny.en-q5_1.bin"

@Singleton
class WhisperSpeechToTextEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : SpeechToTextEngine {

    private var whisperContext: WhisperContext? = null
    private val initMutex = Mutex()

    private suspend fun ensureContext(): WhisperContext {
        whisperContext?.let { return it }
        return initMutex.withLock {
            whisperContext ?: withContext(Dispatchers.IO) {
                WhisperContext.createContextFromAsset(context.assets, MODEL_ASSET_PATH)
            }.also { whisperContext = it }
        }
    }

    override suspend fun transcribe(samples: FloatArray): Result<String> {
        return try {
            val ctx = ensureContext()
            val text = ctx.transcribeData(samples, printTimestamp = false)
            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
