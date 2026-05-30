package com.jugurdzija.homeshelf.data

import android.graphics.Bitmap

data class CaptureData(
    val name: String? = null,
    val bitmap: Bitmap? = null,
    val referenceLabel: String? = null,
    val referenceFilePath: String? = null,
    val similarityScore: Double? = null,
    val similarityThreshold: Double? = null,
    val allMatchScores: Map<String, Double>? = null,
    val framesAnalyzed: Int? = null,
    val captureAttempt: Int? = null,
    val groundTruth: List<GroundTruthCell> = emptyList()
)
