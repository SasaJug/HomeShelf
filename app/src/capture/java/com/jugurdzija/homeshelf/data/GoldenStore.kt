package com.jugurdzija.homeshelf.data

import android.graphics.Bitmap

interface GoldenStore {
    suspend fun loadAll(): List<GoldenItem>
    suspend fun delete(name: String): Boolean
    suspend fun loadIntoHolder(name: String): Boolean
    fun readHolder(): CaptureData

    suspend fun save(
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
        groundTruth: List<GroundTruthCell>
    )
    suspend fun populateHolder(
        bitmap: Bitmap,
        storageId: String,
        referenceLabel: String,
        similarityScore: Double? = null,
        similarityThreshold: Double? = null,
        allMatchScores: Map<String, Double>? = null,
        framesAnalyzed: Int? = null,
        captureAttempt: Int? = null
    )
}
