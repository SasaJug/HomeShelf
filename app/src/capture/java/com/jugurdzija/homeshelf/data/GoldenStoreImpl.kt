package com.jugurdzija.homeshelf.data

import android.graphics.BitmapFactory
import com.jugurdzija.homeshelf.ui.golden.GoldenCaptureHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
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
                GoldenItem(
                    name = subDir.name,
                    referenceLabel = json.optString(GoldenConstants.KEY_REFERENCE_LABEL, subDir.name),
                    timestamp = json.optString(GoldenConstants.KEY_TIMESTAMP, ""),
                    dir = subDir
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
        GoldenCaptureHolder.similarityScore = json.optDouble(GoldenConstants.KEY_SIMILARITY_SCORE).takeIf { !it.isNaN() }
        GoldenCaptureHolder.similarityThreshold = json.optDouble(GoldenConstants.KEY_SIMILARITY_THRESHOLD).takeIf { !it.isNaN() }
        GoldenCaptureHolder.allMatchScores = allScores
        GoldenCaptureHolder.framesAnalyzed = json.optInt(GoldenConstants.KEY_FRAMES_ANALYZED).takeIf { it != 0 }
        GoldenCaptureHolder.captureAttempt = json.optInt(GoldenConstants.KEY_CAPTURE_ATTEMPT)

        true
    }
}
