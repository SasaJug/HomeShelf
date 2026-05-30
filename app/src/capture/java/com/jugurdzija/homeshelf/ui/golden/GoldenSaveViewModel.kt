package com.jugurdzija.homeshelf.ui.golden

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.GoldenConstants
import com.jugurdzija.homeshelf.di.DiConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Named
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
class GoldenSaveViewModel @Inject constructor(
    @Named(DiConstants.NAMED_STORAGE_ROOT) private val storageRoot: File
) : ViewModel() {

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
                val dir = File(storageRoot, "${GoldenConstants.DIR_COMPARISONS}/$name")
                dir.mkdirs()

                FileOutputStream(File(dir, GoldenConstants.FILE_PHOTO)).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
                }

                val allScores = JSONObject()
                holder.allMatchScores?.forEach { (label, score) -> allScores.put(label, score) }

                val meta = JSONObject().apply {
                    put(GoldenConstants.KEY_TIMESTAMP, Instant.now().toString())
                    put(GoldenConstants.KEY_NAME, name)
                    put(GoldenConstants.KEY_REFERENCE_LABEL, holder.referenceLabel)
                    put(GoldenConstants.KEY_SIMILARITY_SCORE, holder.similarityScore)
                    put(GoldenConstants.KEY_SIMILARITY_THRESHOLD, holder.similarityThreshold)
                    put(GoldenConstants.KEY_ALL_MATCH_SCORES, allScores)
                    put(GoldenConstants.KEY_FRAMES_ANALYZED, holder.framesAnalyzed)
                    put(GoldenConstants.KEY_CAPTURE_ATTEMPT, holder.captureAttempt)
                    put(GoldenConstants.KEY_IMAGE_WIDTH, bitmap.width)
                    put(GoldenConstants.KEY_IMAGE_HEIGHT, bitmap.height)
                    put(GoldenConstants.KEY_DEVICE_MODEL, "${Build.MANUFACTURER} ${Build.MODEL}")
                    put(GoldenConstants.KEY_ANDROID_API, Build.VERSION.SDK_INT)
                }

                File(dir, GoldenConstants.FILE_META).writeText(meta.toString(2))
                _saveState.value = SaveState.Saved
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
