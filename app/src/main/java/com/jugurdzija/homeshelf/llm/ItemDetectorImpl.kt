package com.jugurdzija.homeshelf.llm

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.jugurdzija.homeshelf.data.BoundingBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

private const val MODEL_NAME = "gemini-2.5-flash"
private const val REQUEST_TIMEOUT_MS = 30_000L
private const val MAX_IMAGE_DIMENSION = 768
private const val JPEG_QUALITY = 80
private const val POSITION_SCALE = 1000f

private const val ITEM_PROMPT =
    "Detect individual items stored inside the storage unit in this image - the fridge, " +
        "pantry, cabinet, or shelving unit that holds the items. Only detect items inside " +
        "the storage unit's interior opening; ignore the surrounding room, door, walls, or " +
        "anything outside the storage unit. For each item, return a short name a person " +
        "would use for it (e.g. 'Rice', 'Olive oil'), a tight bounding box around it, and " +
        "whether it is a transparent or translucent container (a jar, bottle, or bin you " +
        "can see the contents through). " +
        "The bounding box is [ymin, xmin, ymax, xmax], normalized to the range 0 to 1000 " +
        "relative to the image dimensions, with the origin at the top-left."

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
class ItemDetectorImpl @Inject constructor() : ItemDetector {

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
        text(ITEM_PROMPT)
        image(bitmap.downscaleForModel(MAX_IMAGE_DIMENSION, JPEG_QUALITY))
    }
}
