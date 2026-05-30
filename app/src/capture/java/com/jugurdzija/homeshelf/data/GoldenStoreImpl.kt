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
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class GoldenStoreImpl @Inject constructor(
    @param:Named(DiConstants.NAMED_STORAGE_ROOT) private val storageRoot: File
) : GoldenStore {

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
                    referenceLabel = json.optString(GoldenConstants.KEY_REFERENCE_LABEL, subDir.name),
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

    override suspend fun loadIntoHolder(name: String): Boolean = withContext(Dispatchers.IO) {
        val dir = File(comparisonsDir(), name)
        val metaFile = File(dir, GoldenConstants.FILE_META)
        val photoFile = File(dir, GoldenConstants.FILE_PHOTO)
        if (!metaFile.exists() || !photoFile.exists()) return@withContext false

        val json = try {
            JSONObject(metaFile.readText())
        } catch (e: Exception) {
            return@withContext false
        }
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath) ?: return@withContext false

        val allScores = json.optJSONObject(GoldenConstants.KEY_ALL_MATCH_SCORES)?.let { obj ->
            obj.keys().asSequence().associateWith { key -> obj.getDouble(key) }
        }

        GoldenCaptureHolder.name = json.optString(GoldenConstants.KEY_NAME, name).takeIf { it.isNotEmpty() }
        GoldenCaptureHolder.bitmap = bitmap
        GoldenCaptureHolder.referenceLabel = json.optString(GoldenConstants.KEY_REFERENCE_LABEL).takeIf { it.isNotEmpty() }
        GoldenCaptureHolder.referenceFilePath = json.optString(GoldenConstants.KEY_REFERENCE_FILE_PATH).takeIf { it.isNotEmpty() }
        GoldenCaptureHolder.similarityScore = json.optDouble(GoldenConstants.KEY_SIMILARITY_SCORE).takeIf { !it.isNaN() }
        GoldenCaptureHolder.similarityThreshold = json.optDouble(GoldenConstants.KEY_SIMILARITY_THRESHOLD).takeIf { !it.isNaN() }
        GoldenCaptureHolder.allMatchScores = allScores
        GoldenCaptureHolder.framesAnalyzed = json.optInt(GoldenConstants.KEY_FRAMES_ANALYZED).takeIf { it != 0 }
        GoldenCaptureHolder.captureAttempt = json.optInt(GoldenConstants.KEY_CAPTURE_ATTEMPT)
        GoldenCaptureHolder.groundTruth = parseGroundTruth(json)

        true
    }

    override suspend fun save(
        bitmap: Bitmap,
        name: String,
        referenceLabel: String?,
        referenceFilePath: String?,
        similarityScore: Double?,
        similarityThreshold: Double?,
        allMatchScores: Map<String, Double>?,
        framesAnalyzed: Int?,
        captureAttempt: Int?,
        groundTruth: List<GroundTruthCell>
    ) = withContext(Dispatchers.IO) {
        val dir = File(comparisonsDir(), name)
        dir.mkdirs()

        FileOutputStream(File(dir, GoldenConstants.FILE_PHOTO)).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        val allScoresJson = JSONObject()
        allMatchScores?.forEach { (label, score) -> allScoresJson.put(label, score) }

        val groundTruthArr = JSONArray()
        groundTruth.forEach { cell ->
            groundTruthArr.put(JSONObject().apply {
                put(GoldenConstants.KEY_CELL_INDEX, cell.cellIndex)
                put(GoldenConstants.KEY_CHANGE_TYPE, cell.changeType.name)
            })
        }

        val meta = JSONObject().apply {
            put(GoldenConstants.KEY_TIMESTAMP, Instant.now().toString())
            put(GoldenConstants.KEY_NAME, name)
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

    override fun readHolder(): CaptureData = CaptureData(
        name = GoldenCaptureHolder.name,
        bitmap = GoldenCaptureHolder.bitmap,
        referenceLabel = GoldenCaptureHolder.referenceLabel,
        referenceFilePath = GoldenCaptureHolder.referenceFilePath,
        similarityScore = GoldenCaptureHolder.similarityScore,
        similarityThreshold = GoldenCaptureHolder.similarityThreshold,
        allMatchScores = GoldenCaptureHolder.allMatchScores,
        framesAnalyzed = GoldenCaptureHolder.framesAnalyzed,
        captureAttempt = GoldenCaptureHolder.captureAttempt,
        groundTruth = GoldenCaptureHolder.groundTruth
    )

    override suspend fun populateHolder(
        bitmap: Bitmap,
        referenceLabel: String,
        referenceFilePath: String,
        similarityScore: Double,
        similarityThreshold: Double,
        allMatchScores: Map<String, Double>,
        framesAnalyzed: Int,
        captureAttempt: Int
    ) {
        GoldenCaptureHolder.name = null
        GoldenCaptureHolder.bitmap = bitmap
        GoldenCaptureHolder.referenceLabel = referenceLabel
        GoldenCaptureHolder.referenceFilePath = referenceFilePath
        GoldenCaptureHolder.similarityScore = similarityScore
        GoldenCaptureHolder.similarityThreshold = similarityThreshold
        GoldenCaptureHolder.allMatchScores = allMatchScores
        GoldenCaptureHolder.framesAnalyzed = framesAnalyzed
        GoldenCaptureHolder.captureAttempt = captureAttempt
        GoldenCaptureHolder.groundTruth = emptyList()
    }

    private fun parseGroundTruth(json: JSONObject): List<GroundTruthCell> {
        val arr = json.optJSONArray(GoldenConstants.KEY_GROUND_TRUTH) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            val obj = arr.optJSONObject(i) ?: return@mapNotNull null
            val changeType = try {
                ChangeType.valueOf(obj.getString(GoldenConstants.KEY_CHANGE_TYPE))
            } catch (e: Exception) {
                ChangeType.NO_CHANGE
            }
            GroundTruthCell(
                cellIndex = obj.getInt(GoldenConstants.KEY_CELL_INDEX),
                changeType = changeType
            )
        }
    }
}
