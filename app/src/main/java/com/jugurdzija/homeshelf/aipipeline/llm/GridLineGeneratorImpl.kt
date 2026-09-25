package com.jugurdzija.homeshelf.aipipeline.llm

import android.content.Context
import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.jugurdzija.homeshelf.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds


@Serializable
private data class GridLineJson(val orientation: String, val position: Int)

@Singleton
class GridLineGeneratorImpl @Inject constructor(
    @ApplicationContext private val appContext: Context
) : GridLineGenerator {

    private val gridPrompt: String by lazy { appContext.readPromptResource(R.raw.grid_detection_prompt) }

    private val model: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0f
                responseMimeType = "application/json"
                responseSchema = Schema.array(
                    Schema.obj(
                        properties = mapOf(
                            "orientation" to Schema.enumeration(listOf("h", "v")),
                            "position" to Schema.integer()
                        )
                    )
                )
            }
        )
    }

    override suspend fun generate(bitmap: Bitmap): Result<List<GeneratedGuideLine>> {
        return try {
            withTimeout(REQUEST_TIMEOUT_MS.milliseconds) {
                val prompt = withContext(Dispatchers.Default) { buildPrompt(bitmap) }
                val response = model.generateContent(prompt)
                val json = response.text ?: return@withTimeout Result.failure(
                    IllegalStateException("Empty response from model")
                )
                val lines = Json.decodeFromString<List<GridLineJson>>(json).map {
                    GeneratedGuideLine(
                        isHorizontal = it.orientation == "h",
                        position = it.position.toFloat() / POSITION_SCALE
                    )
                }
                Result.success(lines)
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(bitmap: Bitmap) = content {
        text(gridPrompt)
        image(bitmap.downscaleForModel(MAX_IMAGE_DIMENSION, JPEG_QUALITY))
    }
}
