package com.jugurdzija.homeshelf.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import com.jugurdzija.homeshelf.di.DiConstants
import com.jugurdzija.homeshelf.llm.ItemChange
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class GoldenStoreImpl @Inject constructor(
    @param:Named(DiConstants.NAMED_STORAGE_ROOT) private val storageRoot: File
) : GoldenStore {

    private var holder: CaptureData = CaptureData()

    private fun comparisonsDir() = File(storageRoot, GoldenConstants.DIR_COMPARISONS)

    override suspend fun loadAll(): List<GoldenItem> = withContext(Dispatchers.IO) {
        val dir = comparisonsDir()
        if (!dir.exists()) return@withContext emptyList()
        dir.listFiles()
            ?.filter { it.isDirectory && File(it, GoldenConstants.FILE_META).exists() }
            ?.mapNotNull { subDir ->
                val json = try {
                    JSONObject(File(subDir, GoldenConstants.FILE_META).readText())
                } catch (e: Exception) {
                    return@mapNotNull null
                }
                val groundTruth = parseGroundTruth(json)
                GoldenItem(
                    name = subDir.name,
                    storageId = json.optString(GoldenConstants.KEY_STORAGE_ID).takeIf { it.isNotEmpty() },
                    referenceLabel = json.optString(GoldenConstants.KEY_REFERENCE_LABEL, subDir.name),
                    referenceFilePath = json.optString(GoldenConstants.KEY_REFERENCE_FILE_PATH, ""),
                    timestamp = json.optString(GoldenConstants.KEY_TIMESTAMP, ""),
                    dir = subDir,
                    groundTruth = groundTruth
                )
            }
            ?.sortedByDescending { it.timestamp }
            ?: emptyList()
    }

    override suspend fun delete(name: String): Boolean = withContext(Dispatchers.IO) {
        File(comparisonsDir(), name).deleteRecursively()
    }

    override suspend fun loadDetails(name: String): CaptureData? = withContext(Dispatchers.IO) {
        val dir = File(comparisonsDir(), name)
        val metaFile = File(dir, GoldenConstants.FILE_META)
        val photoFile = File(dir, GoldenConstants.FILE_PHOTO)
        if (!metaFile.exists() || !photoFile.exists()) return@withContext null

        val json = try {
            JSONObject(metaFile.readText())
        } catch (e: Exception) {
            return@withContext null
        }
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath) ?: return@withContext null

        val allScores = json.optJSONObject(GoldenConstants.KEY_ALL_MATCH_SCORES)?.let { obj ->
            obj.keys().asSequence().associateWith { key -> obj.getDouble(key) }
        }

        CaptureData(
            name = json.optString(GoldenConstants.KEY_NAME, name).takeIf { it.isNotEmpty() },
            bitmap = bitmap,
            storageId = json.optString(GoldenConstants.KEY_STORAGE_ID).takeIf { it.isNotEmpty() },
            referenceLabel = json.optString(GoldenConstants.KEY_REFERENCE_LABEL).takeIf { it.isNotEmpty() },
            referenceFilePath = json.optString(GoldenConstants.KEY_REFERENCE_FILE_PATH).takeIf { it.isNotEmpty() },
            similarityScore = json.optDouble(GoldenConstants.KEY_SIMILARITY_SCORE).takeIf { !it.isNaN() },
            similarityThreshold = json.optDouble(GoldenConstants.KEY_SIMILARITY_THRESHOLD).takeIf { !it.isNaN() },
            allMatchScores = allScores,
            framesAnalyzed = json.optInt(GoldenConstants.KEY_FRAMES_ANALYZED).takeIf { it != 0 },
            captureAttempt = json.optInt(GoldenConstants.KEY_CAPTURE_ATTEMPT),
            groundTruth = parseGroundTruth(json)
        )
    }

    override suspend fun save(
        bitmap: Bitmap,
        name: String,
        storageId: String?,
        referenceLabel: String?,
        referenceFilePath: String?,
        similarityScore: Double?,
        similarityThreshold: Double?,
        allMatchScores: Map<String, Double>?,
        framesAnalyzed: Int?,
        captureAttempt: Int?,
        groundTruth: List<GroundTruthItem>
    ) = withContext(Dispatchers.IO) {
        val dir = File(comparisonsDir(), name)
        dir.mkdirs()

        FileOutputStream(File(dir, GoldenConstants.FILE_PHOTO)).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        val allScoresJson = JSONObject()
        allMatchScores?.forEach { (label, score) -> allScoresJson.put(label, score) }

        val groundTruthArr = JSONArray()
        groundTruth.forEach { item ->
            groundTruthArr.put(JSONObject().apply {
                put(GoldenConstants.KEY_ITEM_ID, item.itemId)
                put(GoldenConstants.KEY_ITEM_NAME, item.name)
                put(GoldenConstants.KEY_CHANGE_TYPE, item.changeType.name)
                put(GoldenConstants.KEY_CELL_NAME, item.cellName)
            })
        }

        val meta = JSONObject().apply {
            put(GoldenConstants.KEY_TIMESTAMP, Instant.now().toString())
            put(GoldenConstants.KEY_NAME, name)
            put(GoldenConstants.KEY_STORAGE_ID, storageId)
            put(GoldenConstants.KEY_REFERENCE_LABEL, referenceLabel)
            put(GoldenConstants.KEY_REFERENCE_FILE_PATH, referenceFilePath)
            put(GoldenConstants.KEY_SIMILARITY_SCORE, similarityScore)
            put(GoldenConstants.KEY_SIMILARITY_THRESHOLD, similarityThreshold)
            put(GoldenConstants.KEY_ALL_MATCH_SCORES, allScoresJson)
            put(GoldenConstants.KEY_FRAMES_ANALYZED, framesAnalyzed)
            put(GoldenConstants.KEY_CAPTURE_ATTEMPT, captureAttempt)
            put(GoldenConstants.KEY_IMAGE_WIDTH, bitmap.width)
            put(GoldenConstants.KEY_IMAGE_HEIGHT, bitmap.height)
            put(GoldenConstants.KEY_DEVICE_MODEL, "${Build.MANUFACTURER} ${Build.MODEL}")
            put(GoldenConstants.KEY_ANDROID_API, Build.VERSION.SDK_INT)
            put(GoldenConstants.KEY_GROUND_TRUTH, groundTruthArr)
        }

        File(dir, GoldenConstants.FILE_META).writeText(meta.toString(2))
    }

    override fun readHolder(): CaptureData = holder

    override suspend fun populateHolder(
        bitmap: Bitmap,
        storageId: String,
        referenceLabel: String,
        similarityScore: Double?,
        similarityThreshold: Double?,
        allMatchScores: Map<String, Double>?,
        framesAnalyzed: Int?,
        captureAttempt: Int?
    ) {
        holder = CaptureData(
            bitmap = bitmap,
            storageId = storageId,
            referenceLabel = referenceLabel,
            similarityScore = similarityScore,
            similarityThreshold = similarityThreshold,
            allMatchScores = allMatchScores,
            framesAnalyzed = framesAnalyzed,
            captureAttempt = captureAttempt
        )
    }

    private fun parseGroundTruth(json: JSONObject): List<GroundTruthItem> {
        val arr = json.optJSONArray(GoldenConstants.KEY_GROUND_TRUTH) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val obj = arr.optJSONObject(i) ?: return@mapNotNull null
            val changeType = try {
                ItemChange.valueOf(obj.getString(GoldenConstants.KEY_CHANGE_TYPE))
            } catch (e: Exception) {
                ItemChange.UNCHANGED
            }
            GroundTruthItem(
                itemId = obj.optString(GoldenConstants.KEY_ITEM_ID).takeIf { it.isNotEmpty() && it != "null" },
                name = obj.optString(GoldenConstants.KEY_ITEM_NAME, ""),
                changeType = changeType,
                cellName = obj.optString(GoldenConstants.KEY_CELL_NAME).takeIf { it.isNotEmpty() && it != "null" }
            )
        }
    }
}
