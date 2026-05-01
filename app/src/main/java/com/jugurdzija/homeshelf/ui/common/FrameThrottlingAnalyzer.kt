package com.jugurdzija.homeshelf.ui.common

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.util.concurrent.atomic.AtomicInteger

class FrameThrottlingAnalyzer(
    private val skipFactor: Int = 15,
    private val onFrame: (Bitmap) -> Unit
) : ImageAnalysis.Analyzer {

    private val counter = AtomicInteger(0)

    override fun analyze(image: ImageProxy) {
        try {
            if (counter.getAndIncrement() % skipFactor != 0) return
            val bitmap = image.toBitmap()
            val rotationDegrees = image.imageInfo.rotationDegrees.toFloat()
            val rotated = if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else bitmap
            onFrame(rotated)
        } finally {
            image.close()
        }
    }
}
