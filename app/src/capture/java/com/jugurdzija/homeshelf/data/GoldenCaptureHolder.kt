package com.jugurdzija.homeshelf.data

import android.graphics.Bitmap

internal object GoldenCaptureHolder {
    var name: String? = null
    var bitmap: Bitmap? = null
    var referenceLabel: String? = null
    var referenceFilePath: String? = null
    var similarityScore: Double? = null
    var similarityThreshold: Double? = null
    var allMatchScores: Map<String, Double>? = null
    var framesAnalyzed: Int? = null
    var captureAttempt: Int? = null
    var groundTruth: List<GroundTruthCell> = emptyList()
}
