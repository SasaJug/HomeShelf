package com.jugurdzija.homeshelf.ui.golden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.CaptureData
import com.jugurdzija.homeshelf.data.ChangeType
import com.jugurdzija.homeshelf.data.GoldenStore
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.GuideLineStore
import com.jugurdzija.homeshelf.data.GroundTruthCell
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoldenSaveViewModel @Inject constructor(
    private val goldenStore: GoldenStore,
    private val guideLineStore: GuideLineStore
) : ViewModel() {

    data class GoldenSaveUiState(
        val name: String = "",
        val captureData: CaptureData = CaptureData(),
        val annotations: Map<Int, ChangeType> = emptyMap()
    )

    sealed interface SaveState {
        data object Idle : SaveState
        data object Saving : SaveState
        data object Saved : SaveState
        data class Error(val message: String) : SaveState
    }

    sealed interface GuideLineState {
        data object Loading : GuideLineState
        data class Ready(val guideLines: List<GuideLine>) : GuideLineState
        data object Unavailable : GuideLineState
    }

    private val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))

    private val _uiState = MutableStateFlow(
        goldenStore.readHolder().let { data ->
            GoldenSaveUiState(
                name = data.name ?: "${data.referenceLabel ?: "unknown"}_$timestamp",
                captureData = data
            )
        }
    )
    val uiState: StateFlow<GoldenSaveUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _guideLineState = MutableStateFlow<GuideLineState>(GuideLineState.Loading)
    val guideLineState: StateFlow<GuideLineState> = _guideLineState.asStateFlow()

    fun setName(value: String) = _uiState.update { it.copy(name = value) }

    fun setAnnotation(cellIndex: Int, changeType: ChangeType) =
        _uiState.update { it.copy(annotations = it.annotations + (cellIndex to changeType)) }

    fun loadGuideLines() {
        val refPath = _uiState.value.captureData.referenceFilePath ?: run {
            _guideLineState.value = GuideLineState.Unavailable
            return
        }
        viewModelScope.launch {
            val lines = guideLineStore.load(refPath)
            _guideLineState.value = if (lines.size >= 4) GuideLineState.Ready(lines)
            else GuideLineState.Unavailable
        }
    }

    fun save() {
        val state = _uiState.value
        val bitmap = state.captureData.bitmap ?: run {
            _saveState.value = SaveState.Error("No image to save")
            return
        }
        val groundTruth = buildGroundTruth(state.annotations)
        val name = state.name.trim()
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                goldenStore.save(
                    bitmap = bitmap,
                    name = name,
                    referenceLabel = state.captureData.referenceLabel,
                    referenceFilePath = state.captureData.referenceFilePath,
                    similarityScore = state.captureData.similarityScore,
                    similarityThreshold = state.captureData.similarityThreshold,
                    allMatchScores = state.captureData.allMatchScores,
                    framesAnalyzed = state.captureData.framesAnalyzed,
                    captureAttempt = state.captureData.captureAttempt,
                    groundTruth = groundTruth
                )
                _saveState.value = SaveState.Saved
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun buildGroundTruth(annotations: Map<Int, ChangeType>): List<GroundTruthCell> {
        val lines = (_guideLineState.value as? GuideLineState.Ready)?.guideLines ?: return emptyList()
        val hLines = lines.filter { it.isHorizontal }.sortedBy { it.position }
        val vLines = lines.filter { !it.isHorizontal }.sortedBy { it.position }
        val numRows = hLines.size - 1
        val numCols = vLines.size - 1
        return (0 until numRows * numCols).map { index ->
            GroundTruthCell(cellIndex = index, changeType = annotations[index] ?: ChangeType.NO_CHANGE)
        }
    }
}
