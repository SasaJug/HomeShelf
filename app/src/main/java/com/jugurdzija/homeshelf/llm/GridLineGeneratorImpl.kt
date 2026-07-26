package com.jugurdzija.homeshelf.llm

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val MODEL_NAME = "gemini-2.5-flash"
private const val REQUEST_TIMEOUT_MS = 30_000L
private const val MAX_IMAGE_DIMENSION = 768
private const val JPEG_QUALITY = 80
private const val POSITION_SCALE = 1000

private const val GRID_PROMPT =
    "Detect the shelf grid of the storage unit in this image - the fridge, pantry, cabinet, " +
        "or shelving unit that holds the items. The grid is made of straight lines: the outer " +
        "boundary of the storage unit's interior opening (its topmost shelf or ceiling, its " +
        "bottommost shelf or floor, and its left and right inner walls), plus any interior lines " +
        "that divide the space into separate shelves - usually horizontal shelf boards, but " +
        "include vertical dividers too if the unit is split into columns. " +
        "Do not include the surrounding room, door, or frame outside the storage unit, and do not " +
        "return a box - return each boundary edge and each shelf divider as its own straight line. " +
        "For each line, return its orientation ('h' for a horizontal line spanning left-right, " +
        "'v' for a vertical line spanning top-bottom) and its position: for a horizontal line, " +
        "the y-coordinate where it crosses the image; for a vertical line, the x-coordinate. " +
        "Positions are normalized to the range 0 to 1000 relative to the image dimensions, with " +
        "the origin at the top-left."

@Serializable
private data class GridLineJson(val orientation: String, val position: Int)

@Singleton
class GridLineGeneratorImpl @Inject constructor() : GridLineGenerator {

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
            withTimeout(REQUEST_TIMEOUT_MS) {
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
        text(GRID_PROMPT)
        image(bitmap.downscaleForModel(MAX_IMAGE_DIMENSION, JPEG_QUALITY))
    }
}
