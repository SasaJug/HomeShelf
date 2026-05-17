package com.jugurdzija.homeshelf.util

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedder

class ImageEmbedderHelper(
    private val context: Context,
    var currentDelegate: Int = DELEGATE_CPU,
    var currentModel: Int = MODEL_SMALL,
    var listener: EmbedderListener? = null
) {
    private var imageEmbedder: ImageEmbedder? = null

    init {
        setupImageEmbedder()
    }

    fun setupImageEmbedder() {
        val baseOptionsBuilder = BaseOptions.builder()
        when (currentDelegate) {
            DELEGATE_CPU -> {
                baseOptionsBuilder.setDelegate(Delegate.CPU)
            }
            DELEGATE_GPU -> {
                baseOptionsBuilder.setDelegate(Delegate.GPU)
            }
        }
        when (currentModel) {
            MODEL_SMALL -> {
                baseOptionsBuilder.setModelAssetPath(MODEL_SMALL_PATH)
            }
            MODEL_LARGE -> {
                baseOptionsBuilder.setModelAssetPath(MODEL_LARGE_PATH)
            }
        }
        try {
            val baseOptions = baseOptionsBuilder.build()
            val optionsBuilder =
                ImageEmbedder.ImageEmbedderOptions.builder().setBaseOptions(baseOptions)
            val options = optionsBuilder.build()
            imageEmbedder = ImageEmbedder.createFromOptions(context, options)
        } catch (e: IllegalStateException) {
            listener?.onError(
                "Image embedder failed to initialize. See error logs for " +
                        "details"
            )
            Log.e(
                TAG,
                "Image embedder failed to load model with error: " + e.message
            )
        } catch (e: RuntimeException) {
            // This occurs if the model being used does not support GPU
            listener?.onError(
                "Image embedder failed to initialize. See error logs for " +
                        "details", GPU_ERROR
            )
            Log.e(
                TAG,
                "Image embedder failed to load model with error: " + e.message
            )
        }
    }

    fun embed(firstBitmap: Bitmap, secondBitmap: Bitmap): ResultBundle? {
        val startTime = SystemClock.uptimeMillis()

        val firstMpImage = BitmapImageBuilder(firstBitmap).build()
        val secondMpImage = BitmapImageBuilder(secondBitmap).build()
        imageEmbedder?.let {
            val firstEmbed =
                it.embed(firstMpImage).embeddingResult().embeddings().first()
            val secondEmbed =
                it.embed(secondMpImage).embeddingResult().embeddings().first()
            val inferenceTimeMs = SystemClock.uptimeMillis() - startTime
            return ResultBundle(
                ImageEmbedder.cosineSimilarity(firstEmbed, secondEmbed),
                inferenceTimeMs
            )
        }
        return null
    }

    fun embedSingle(bitmap: Bitmap): FloatArray? {
        val mpImage = BitmapImageBuilder(bitmap).build()
        return imageEmbedder
            ?.embed(mpImage)
            ?.embeddingResult()
            ?.embeddings()
            ?.firstOrNull()
            ?.floatEmbedding()
    }

    fun clearImageEmbedder() {
        imageEmbedder?.close()
        imageEmbedder = null
    }

    data class ResultBundle(
        val similarity: Double,
        val inferenceTime: Long,
    )

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val MODEL_SMALL = 0
        const val MODEL_LARGE = 1
        const val MODEL_SMALL_PATH = "mobilenet_v3_small.tflite"
        const val MODEL_LARGE_PATH = "mobilenet_v3_large.tflite"
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
        private const val TAG = "ImageEmbedderHelper"
    }

    interface EmbedderListener {
        fun onError(error: String, errorCode: Int = OTHER_ERROR)
    }
}
