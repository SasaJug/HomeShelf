package com.jugurdzija.homeshelf.data

interface GoldenStore {
    suspend fun loadAll(): List<GoldenItem>
    suspend fun delete(name: String): Boolean
    suspend fun loadIntoHolder(name: String): Boolean
    fun readHolder(): CaptureData
    suspend fun save(
        bitmap: android.graphics.Bitmap,
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
        bitmap: android.graphics.Bitmap,
        referenceLabel: String,
        referenceFilePath: String,
        similarityScore: Double,
        similarityThreshold: Double,
        allMatchScores: Map<String, Double>,
        framesAnalyzed: Int,
        captureAttempt: Int
    )
    suspend fun populateHolderForStorage(
        bitmap: android.graphics.Bitmap,
        storageId: String,
        referenceLabel: String
    )
}
