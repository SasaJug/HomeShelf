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
import com.jugurdzija.homeshelf.domain.model.BoundingBox
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
internal data class DetectedItemJson(
    val name: String,
    val box: List<Int>,
    val isTransparentContainer: Boolean
)

internal fun DetectedItemJson.toDetectedItem(): DetectedItem? {
    if (box.size != 4) return null
    val (yMin, xMin, yMax, xMax) = box
    return DetectedItem(
        name = name,
        box = BoundingBox(
            x = xMin / POSITION_SCALE,
            y = yMin / POSITION_SCALE,
            width = (xMax - xMin) / POSITION_SCALE,
            height = (yMax - yMin) / POSITION_SCALE
        ),
        isTransparentContainer = isTransparentContainer
    )
}

@Singleton
class ItemDetectorImpl @Inject constructor(
    @ApplicationContext private val appContext: Context
) : ItemDetector {

    private val itemPrompt: String by lazy { appContext.readPromptResource(R.raw.item_detection_prompt) }

    private val model: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0f
                responseMimeType = "application/json"
                responseSchema = Schema.array(
                    Schema.obj(
                        properties = mapOf(
                            "name" to Schema.string(),
                            "box" to Schema.array(Schema.integer()),
                            "isTransparentContainer" to Schema.boolean()
                        )
                    )
                )
            }
        )
    }

    override suspend fun detect(bitmap: Bitmap): Result<List<DetectedItem>> {
        return try {
            withTimeout(REQUEST_TIMEOUT_MS.milliseconds) {
                val prompt = withContext(Dispatchers.Default) { buildPrompt(bitmap) }
                val response = model.generateContent(prompt)
                val json = response.text ?: return@withTimeout Result.failure(
                    IllegalStateException("Empty response from model")
                )
                val items = Json.decodeFromString<List<DetectedItemJson>>(json).mapNotNull {
                    it.toDetectedItem()
                }
                Result.success(items)
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(bitmap: Bitmap) = content {
        text(itemPrompt)
        image(bitmap.downscaleForModel(MAX_IMAGE_DIMENSION, JPEG_QUALITY))
    }
}
