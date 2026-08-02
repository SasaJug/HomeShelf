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
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
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
        "the known items already recorded in that cell — each also outlined with a plain blue " +
        "box (no label or number) in the reference image, at the position given by its box in " +
        "the JSON.\n" +
        "For every known item, look specifically at its own region in the new image — one item at " +
        "a time, not a single glance over the whole shelf — and report what happened to it. Do not " +
        "invent or assign any id — you identify which known item you mean purely by reporting a " +
        "box at (or close to) that item's known position; matching each report back to the correct " +
        "known item is done afterwards by comparing box positions, not by any id or label.\n" +
        "Do not default to UNCHANGED just because an item was in the known list. Removal is the " +
        "single most common change in a restock, so for every known item, actively check whether " +
        "it is still visibly present at that region before deciding: if the region now shows empty " +
        "shelf space, a bare shelf behind where the item used to be, or a different item, that is " +
        "not UNCHANGED — report REMOVED, REPLACED, or the appropriate fill-level change instead. " +
        "Only report UNCHANGED when you can actually see that same item still occupying that " +
        "region in the new image.\n" +
        "If you see an item in the new image that isn't one of the known items, report it too, as " +
        "a newly seen item.\n" +
        "For each item, report:\n" +
        "- box: tight bounding box of the item's current extent in the new image, or its last " +
        "known extent if it's now gone. Same [ymin, xmin, ymax, xmax] normalized 0-1000 " +
        "convention as the known items' boxes, relative to this cell's image. Always required.\n" +
        "- change: one of UNCHANGED, REMOVED (item physically gone), REPLACED (a different item " +
        "now occupies the same spot), ADDED (newly seen item, not one of the known items), " +
        "PARTIALLY_CONSUMED (transparent container, fill level clearly and significantly dropped " +
        "but not empty), " +
        "FULLY_CONSUMED (transparent container, now empty, container still present), " +
        "PARTIALLY_FILLED (transparent container, fill level clearly and significantly increased " +
        "but not full), " +
        "FULLY_FILLED (transparent container, topped up to full), or UNKNOWN (can't tell, e.g. " +
        "occlusion or a bad angle — use this rather than guessing).\n" +
        "- description: a brief note on what you saw and why you picked that change value.\n" +
        "The four fill-level values only apply to transparent containers — never use them for an " +
        "item that isn't one. Only use PARTIALLY_CONSUMED or PARTIALLY_FILLED when the fill level " +
        "difference is large enough to be obvious at a glance — small or ambiguous variations that " +
        "could just be lighting/angle differences should be reported as UNCHANGED instead.\n" +
        "For an ADDED or REPLACED item, also report: name (short, human-readable name of the " +
        "item now occupying that spot) and isTransparentContainer.\n" +
        "Respond with one entry per cell, each containing the full list of item results for " +
        "that cell."

@Serializable
private data class KnownItemJson(
    val name: String,
    val isTransparentContainer: Boolean,
    val box: List<Int>? = null
)

@Serializable
private data class ItemDiffJson(
    val change: ItemChange,
    val description: String,
    val box: List<Int>,
    val name: String? = null,
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
                                        "change" to Schema.enumeration(ItemChange.entries.map { it.name }),
                                        "description" to Schema.string(),
                                        "box" to Schema.array(Schema.integer()),
                                        "name" to Schema.string(nullable = true),
                                        "isTransparentContainer" to Schema.boolean(nullable = true)
                                    ),
                                    optionalProperties = listOf("name", "isTransparentContainer")
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
                val knownItemsByCell = cells.associate { it.cellId to it.knownItems }
                val results = Json.decodeFromString<List<CellDiffJson>>(json).map { cellDiff ->
                    cellDiff.toCellDiffResult(knownItemsByCell[cellDiff.cellId].orEmpty())
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
                pair.referenceBitmap.withMarkedBoxes(pair.knownItems)
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

private fun CellDiffJson.toCellDiffResult(knownItems: List<KnownItem>): CellDiffResult {
    val unmatched = knownItems.toMutableList()
    var newItemCount = 0
    val results = items.mapNotNull { itemJson ->
        val box = itemJson.box.toBoundingBox() ?: return@mapNotNull null
        if (itemJson.change == ItemChange.ADDED) {
            newItemCount++
            return@mapNotNull itemJson.toItemDiffResult(
                id = "$NEW_ITEM_ID_PREFIX$newItemCount",
                box = box,
                isContainer = itemJson.isTransparentContainer == true
            )
        }
        val match = unmatched.bestMatchFor(box) ?: return@mapNotNull null
        unmatched.remove(match)
        itemJson.toItemDiffResult(id = match.id, box = box, isContainer = match.isTransparentContainer)
    }
    return CellDiffResult(cellId = cellId, items = results)
}

// Since the model no longer echoes a known item's id, each reported region is matched back to
// the known item whose box overlaps it most (falling back to nearest center when boxes don't
// overlap at all, e.g. a container that shrank to empty).
private fun List<KnownItem>.bestMatchFor(box: BoundingBox): KnownItem? {
    val candidates = mapNotNull { item -> item.box?.let { item to it } }
    if (candidates.isEmpty()) return null
    val byOverlap = candidates.maxByOrNull { (_, itemBox) -> itemBox.iou(box) }
    return if (byOverlap != null && byOverlap.second.iou(box) > 0f) {
        byOverlap.first
    } else {
        candidates.minByOrNull { (_, itemBox) -> itemBox.centerDistanceTo(box) }?.first
    }
}

private fun BoundingBox.iou(other: BoundingBox): Float {
    val left = max(x, other.x)
    val top = max(y, other.y)
    val right = min(x + width, other.x + other.width)
    val bottom = min(y + height, other.y + other.height)
    val intersection = (right - left).coerceAtLeast(0f) * (bottom - top).coerceAtLeast(0f)
    val union = width * height + other.width * other.height - intersection
    return if (union <= 0f) 0f else intersection / union
}

private fun BoundingBox.centerDistanceTo(other: BoundingBox): Float {
    val dx = (x + width / 2f) - (other.x + other.width / 2f)
    val dy = (y + height / 2f) - (other.y + other.height / 2f)
    return hypot(dx.toDouble(), dy.toDouble()).toFloat()
}

private fun ItemDiffJson.toItemDiffResult(id: String, box: BoundingBox, isContainer: Boolean): ItemDiffResult {
    val resolvedChange = if (change in FillLevelChanges && !isContainer) ItemChange.UNKNOWN else change
    return ItemDiffResult(
        id = id,
        change = resolvedChange,
        description = description,
        name = name,
        box = box,
        isTransparentContainer = isTransparentContainer
    )
}
