package com.jugurdzija.homeshelf.stt

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import javax.inject.Inject
import javax.inject.Singleton

private const val MODEL_ASSET_PATH = "model/vosk-model-small-en-us-0.15"
private const val SAMPLE_RATE = 16000f

@Singleton
class VoskSpeechToTextEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : SpeechToTextEngine {

    private var model: Model? = null
    private val initMutex = Mutex()

    private suspend fun ensureModel(): Model {
        model?.let { return it }
        return initMutex.withLock {
            model ?: withContext(Dispatchers.IO) {
                val modelPath = StorageService.sync(context, MODEL_ASSET_PATH, MODEL_ASSET_PATH)
                Model(modelPath)
            }.also { model = it }
        }
    }

    override suspend fun transcribe(samples: FloatArray): Result<String> {
        return try {
            val voskModel = ensureModel()
            val text = withContext(Dispatchers.IO) {
                Recognizer(voskModel, SAMPLE_RATE).use { recognizer ->
                    recognizer.acceptWaveForm(samples, samples.size)
                    Json.parseToJsonElement(recognizer.finalResult)
                        .jsonObject["text"]
                        ?.jsonPrimitive
                        ?.content
                        .orEmpty()
                }
            }
            Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
