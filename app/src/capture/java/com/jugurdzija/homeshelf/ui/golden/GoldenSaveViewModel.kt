package com.jugurdzija.homeshelf.ui.golden

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class GoldenSaveViewModel @Inject constructor() : ViewModel() {

    sealed interface SaveState {
        data object Idle : SaveState
        data object Saving : SaveState
        data object Saved : SaveState
        data class Error(val message: String) : SaveState
    }

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    fun save(context: Context, name: String) {
        val holder = GoldenCaptureHolder
        val bitmap = holder.bitmap ?: run {
            _saveState.value = SaveState.Error("No image to save")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _saveState.value = SaveState.Saving
            try {
                val dir = File(context.filesDir, "golden/comparisons/$name")
                dir.mkdirs()

                FileOutputStream(File(dir, "photo.jpg")).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
                }

                val allScores = JSONObject()
                holder.allMatchScores?.forEach { (label, score) -> allScores.put(label, score) }

                val meta = JSONObject().apply {
                    put("timestamp", Instant.now().toString())
                    put("name", name)
                    put("reference_label", holder.referenceLabel)
                    put("similarity_score", holder.similarityScore)
                    put("similarity_threshold", holder.similarityThreshold)
                    put("all_match_scores", allScores)
                    put("frames_analyzed", holder.framesAnalyzed)
                    put("capture_attempt", holder.captureAttempt)
                    put("image_width", bitmap.width)
                    put("image_height", bitmap.height)
                    put("device_model", "${Build.MANUFACTURER} ${Build.MODEL}")
                    put("android_api", Build.VERSION.SDK_INT)
                }

                File(dir, "meta.json").writeText(meta.toString(2))
                _saveState.value = SaveState.Saved
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
