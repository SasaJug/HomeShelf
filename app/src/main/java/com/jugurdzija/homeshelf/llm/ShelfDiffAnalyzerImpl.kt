package com.jugurdzija.homeshelf.llm

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import androidx.core.graphics.scale

private const val MODEL_NAME = "gemini-2.5-flash"
private const val REQUEST_TIMEOUT_MS = 30_000L
private const val MAX_IMAGE_DIMENSION = 768
private const val JPEG_QUALITY = 80

@Singleton
class ShelfDiffAnalyzerImpl @Inject constructor() : ShelfDiffAnalyzer {

    private val model: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = Schema.array(
                    Schema.obj(
                        properties = mapOf(
                            "cellId" to Schema.string(),
                            "changed" to Schema.boolean(),
                            "description" to Schema.string()
                        )
                    )
                )
            }
        )
    }

    override suspend fun analyze(cells: List<CellPair>): Result<List<CellDiffResult>> {
        return try {
            withTimeout(REQUEST_TIMEOUT_MS) {
                val prompt = withContext(Dispatchers.Default) { buildPrompt(cells) }
                val response = model.generateContent(prompt)
                val json = response.text ?: return@withTimeout Result.failure(
                    IllegalStateException("Empty response from model")
                )
                Result.success(Json.decodeFromString<List<CellDiffResult>>(json))
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(cells: List<CellPair>) = content {
        text(
            "You are comparing shelf photos before and after a restock. For each cell below, " +
                "report whether the contents changed and briefly describe what changed."
        )
        cells.forEach { pair ->
            text("Cell ${pair.cellId} — reference:")
            image(pair.referenceBitmap.toAnalysisBitmap())
            text("Cell ${pair.cellId} — new:")
            image(pair.newBitmap.toAnalysisBitmap())
        }
    }

    private fun Bitmap.toAnalysisBitmap(): Bitmap {
        val scale = MAX_IMAGE_DIMENSION.toFloat() / max(width, height)
        val scaled = if (scale < 1f) {
            this.scale(
                (width * scale).toInt().coerceAtLeast(1),
                (height * scale).toInt().coerceAtLeast(1)
            )
        } else {
            this
        }
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        val bytes = output.toByteArray()
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}
