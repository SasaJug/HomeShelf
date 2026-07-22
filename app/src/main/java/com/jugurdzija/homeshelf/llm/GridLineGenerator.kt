package com.jugurdzija.homeshelf.llm

import android.graphics.Bitmap

data class GeneratedGuideLine(val isHorizontal: Boolean, val position: Float)

interface GridLineGenerator {
    suspend fun generate(bitmap: Bitmap): Result<List<GeneratedGuideLine>>
}
