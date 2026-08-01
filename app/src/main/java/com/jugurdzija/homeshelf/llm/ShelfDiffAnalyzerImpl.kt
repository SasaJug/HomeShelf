package com.jugurdzija.homeshelf.llm

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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

private const val MODEL_NAME = "gemini-2.5-flash"
private const val REQUEST_TIMEOUT_MS = 30_000L
private const val MAX_IMAGE_DIMENSION = 768
private const val JPEG_QUALITY = 80
private const val POSITION_SCALE = 1000f
private const val NEW_ITEM_ID_PREFIX = "N"

private val FillLevelChanges = setOf(
    ItemChange.PARTIALLY_CONSUMED,
    ItemChange.FULLY_CONSUMED,
    ItemChange.PARTIALLY_FILLED,
    ItemChange.FULLY_FILLED
)

private const val PROMPT_INTRO =
    "You are comparing shelf photos before and after a restock, cell by cell. For each cell " +
        "you are given a reference image (before) and a new image (after), plus a JSON list of " +
        "the known items already recorded in that cell — each also outlined with a labeled box " +
        "in the reference image, where the label is the item's id.\n" +
        "For every known item, report what happened to it using its existing id. If you see an " +
        "item in the new image that isn't in the known list, report it too, with a new id of " +
        "the form N1, N2, N3... (one per new item, scoped to this request).\n" +
        "For each item, report:\n" +
        "- id: echo the known item's id, or an N-prefixed id for a newly seen item.\n" +
        "- change: one of UNCHANGED, REMOVED (item physically gone), REPLACED (a different item " +
        "now occupies the same spot), ADDED (new item, id must be N-prefixed), " +
        "PARTIALLY_CONSUMED (transparent container, fill level dropped but not empty), " +
        "FULLY_CONSUMED (transparent container, now empty, container still present), " +
        "PARTIALLY_FILLED (transparent container, fill level increased but not full), " +
        "FULLY_FILLED (transparent container, topped up to full), or UNKNOWN (can't tell, e.g. " +
        "occlusion or a bad angle — use this rather than guessing).\n" +
        "- description: a brief note on what you saw and why you picked that change value.\n" +
        "The four fill-level values only apply to transparent containers — never use them for an " +
        "item that isn't one.\n" +
        "For an ADDED or REPLACED item, also report: name (short, human-readable name of the " +
        "item now occupying that spot), box (tight bounding box, same [ymin, xmin, ymax, xmax] " +
        "normalized 0-1000 convention as the known items' boxes, relative to this cell's image), " +
        "and isTransparentContainer.\n" +
        "Respond with one entry per cell, each containing the full list of item results for " +
        "that cell."

@Serializable
private data class KnownItemJson(
    val id: String,
    val name: String,
    val isTransparentContainer: Boolean,
    val box: List<Int>? = null
)

@Serializable
private data class ItemDiffJson(
    val id: String,
    val change: ItemChange,
    val description: String,
    val name: String? = null,
    val box: List<Int>? = null,
    val isTransparentContainer: Boolean? = null
)

@Serializable
private data class CellDiffJson(
    val cellId: String,
    val items: List<ItemDiffJson>
)

@Singleton
class ShelfDiffAnalyzerImpl @Inject constructor() : ShelfDiffAnalyzer {

    private val model: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0f
                responseMimeType = "application/json"
                responseSchema = Schema.array(
                    Schema.obj(
                        properties = mapOf(
                            "cellId" to Schema.string(),
                            "items" to Schema.array(
                                Schema.obj(
                                    properties = mapOf(
                                        "id" to Schema.string(),
                                        "change" to Schema.enumeration(ItemChange.entries.map { it.name }),
                                        "description" to Schema.string(),
                                        "name" to Schema.string(nullable = true),
                                        "box" to Schema.array(Schema.integer(), nullable = true),
                                        "isTransparentContainer" to Schema.boolean(nullable = true)
                                    ),
                                    optionalProperties = listOf("name", "box", "isTransparentContainer")
                                )
                            )
                        )
                    )
                )
            }
        )
    }

    override suspend fun analyze(cells: List<CellPair>): Result<List<CellDiffResult>> {
        return try {
            withTimeout(REQUEST_TIMEOUT_MS.milliseconds) {
                val prompt = withContext(Dispatchers.Default) { buildPrompt(cells) }
                val response = model.generateContent(prompt)
                val json = response.text ?: return@withTimeout Result.failure(
                    IllegalStateException("Empty response from model")
                )
                val containerIdsByCell = cells.associate { pair ->
                    pair.cellId to pair.knownItems.filter { it.isTransparentContainer }.map { it.id }.toSet()
                }
                val results = Json.decodeFromString<List<CellDiffJson>>(json).map { cellDiff ->
                    cellDiff.toCellDiffResult(containerIdsByCell[cellDiff.cellId].orEmpty())
                }
                Result.success(results)
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(cells: List<CellPair>) = content {
        text(PROMPT_INTRO)
        cells.forEach { pair ->
            text("Cell ${pair.cellId} — reference:")
            val boxedCount = pair.knownItems.count { it.box != null }
            val referenceImage = if (boxedCount > 0) {
                pair.referenceBitmap.withLabeledBoxes(pair.knownItems)
            } else {
                pair.referenceBitmap
            }
            if (pair.knownItems.isNotEmpty()) {
                val knownItemsJson = Json.encodeToString(pair.knownItems.map { it.toKnownItemJson() })
                text("Known items in this cell: $knownItemsJson")
            }
            image(referenceImage.downscaleForModel(MAX_IMAGE_DIMENSION, JPEG_QUALITY))
            text("Cell ${pair.cellId} — new:")
            image(pair.newBitmap.downscaleForModel(MAX_IMAGE_DIMENSION, JPEG_QUALITY))
        }
    }
}

private fun KnownItem.toKnownItemJson() = KnownItemJson(
    id = id,
    name = name,
    isTransparentContainer = isTransparentContainer,
    box = box?.toGeminiBox()
)

private fun BoundingBox.toGeminiBox(): List<Int> {
    val xMin = (x * POSITION_SCALE).toInt()
    val yMin = (y * POSITION_SCALE).toInt()
    val xMax = ((x + width) * POSITION_SCALE).toInt()
    val yMax = ((y + height) * POSITION_SCALE).toInt()
    return listOf(yMin, xMin, yMax, xMax)
}

private fun List<Int>.toBoundingBox(): BoundingBox? {
    if (size != 4) return null
    val (yMin, xMin, yMax, xMax) = this
    return BoundingBox(
        x = xMin / POSITION_SCALE,
        y = yMin / POSITION_SCALE,
        width = (xMax - xMin) / POSITION_SCALE,
        height = (yMax - yMin) / POSITION_SCALE
    )
}

private fun CellDiffJson.toCellDiffResult(containerIds: Set<String>): CellDiffResult =
    CellDiffResult(cellId = cellId, items = items.map { it.toItemDiffResult(containerIds) })

private fun ItemDiffJson.toItemDiffResult(containerIds: Set<String>): ItemDiffResult {
    val isContainer = if (id.startsWith(NEW_ITEM_ID_PREFIX)) isTransparentContainer == true else id in containerIds
    val change = if (change in FillLevelChanges && !isContainer) ItemChange.UNKNOWN else change
    return ItemDiffResult(
        id = id,
        change = change,
        description = description,
        name = name,
        box = box?.toBoundingBox(),
        isTransparentContainer = isTransparentContainer
    )
}
