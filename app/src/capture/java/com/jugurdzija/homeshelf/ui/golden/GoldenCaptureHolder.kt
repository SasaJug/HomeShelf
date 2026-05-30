package com.jugurdzija.homeshelf.ui.golden

import android.graphics.Bitmap

object GoldenCaptureHolder {
    var name: String? = null
    var bitmap: Bitmap? = null
    var referenceLabel: String? = null
    var similarityScore: Double? = null
    var similarityThreshold: Double? = null
    var allMatchScores: Map<String, Double>? = null
    var framesAnalyzed: Int? = null
    var captureAttempt: Int? = null
}
