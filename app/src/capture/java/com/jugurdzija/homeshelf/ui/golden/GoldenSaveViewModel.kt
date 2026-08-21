package com.jugurdzija.homeshelf.ui.golden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.BoundingBox
import com.jugurdzija.homeshelf.data.CaptureData
import com.jugurdzija.homeshelf.data.GoldenStore
import com.jugurdzija.homeshelf.data.GroundTruthItem
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.StorageRepository
import com.jugurdzija.homeshelf.llm.ItemChange
import com.jugurdzija.homeshelf.util.resolveCellName
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KnownAnnotationBox(
    val itemId: String,
    val name: String,
    val boundingBox: BoundingBox,
    val isTransparentContainer: Boolean,
    val fillState: ItemChange? = null
)

data class NewAnnotationBox(
    val localId: String = UUID.randomUUID().toString(),
    val name: String,
    val boundingBox: BoundingBox
)

@HiltViewModel
class GoldenSaveViewModel @Inject constructor(
    private val goldenStore: GoldenStore,
    private val storageRepository: StorageRepository
) : ViewModel() {

    data class GoldenSaveUiState(
        val captureData: CaptureData = CaptureData(),
        val guideLines: List<GuideLine> = emptyList(),
        val knownBoxes: List<KnownAnnotationBox> = emptyList(),
        val newBoxes: List<NewAnnotationBox> = emptyList(),
        val selectedId: String? = null,
        val referenceDataLoaded: Boolean = false
    )

    sealed interface SaveState {
        data object Idle : SaveState
        data object Saving : SaveState
        data object Saved : SaveState
        data class Error(val message: String) : SaveState
    }

    private val storageId: String? = goldenStore.readHolder().storageId

    private val name: String = "${goldenStore.readHolder().referenceLabel ?: "unknown"}_${
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    }"

     private val removedKnownItems = mutableMapOf<String, String>()

    private val _uiState = MutableStateFlow(
        GoldenSaveUiState(captureData = goldenStore.readHolder())
    )
    val uiState: StateFlow<GoldenSaveUiState> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    init {
        loadReferenceData()
    }

    private fun loadReferenceData() {
        val id = storageId ?: run {
            _uiState.update { it.copy(referenceDataLoaded = true) }
            return
        }
        viewModelScope.launch {
            val data = storageRepository.loadLatestData(id)
            _uiState.update {
                it.copy(
                    guideLines = data.guideLines,
                    knownBoxes = data.markedItems.map { item ->
                        KnownAnnotationBox(
                            itemId = item.id,
                            name = item.name,
                            boundingBox = item.boundingBox,
                            isTransparentContainer = item.isTransparentContainer
                        )
                    },
                    referenceDataLoaded = true
                )
            }
        }
    }

    fun select(id: String) {
        _uiState.update { it.copy(selectedId = id) }
    }

    fun createNewBox(box: BoundingBox) {
        val draft = NewAnnotationBox(name = "", boundingBox = box)
        _uiState.update { it.copy(newBoxes = it.newBoxes + draft, selectedId = draft.localId) }
    }

    fun updateBoundingBox(id: String, box: BoundingBox) {
        _uiState.update { state ->
            state.copy(
                knownBoxes = state.knownBoxes.map { if (it.itemId == id) it.copy(boundingBox = box) else it },
                newBoxes = state.newBoxes.map { if (it.localId == id) it.copy(boundingBox = box) else it }
            )
        }
    }

    fun updateNewBoxName(localId: String, name: String) {
        _uiState.update { state ->
            state.copy(newBoxes = state.newBoxes.map { if (it.localId == localId) it.copy(name = name) else it })
        }
    }

    fun setFillState(itemId: String, fillState: ItemChange?) {
        _uiState.update { state ->
            state.copy(
                knownBoxes = state.knownBoxes.map {
                    if (it.itemId == itemId) it.copy(fillState = if (it.fillState == fillState) null else fillState) else it
                }
            )
        }
    }

    fun deleteSelected() {
        val id = _uiState.value.selectedId ?: return
        val known = _uiState.value.knownBoxes.firstOrNull { it.itemId == id }
        if (known != null) removedKnownItems[known.itemId] = known.name
        _uiState.update { state ->
            state.copy(
                knownBoxes = state.knownBoxes.filterNot { it.itemId == id },
                newBoxes = state.newBoxes.filterNot { it.localId == id },
                selectedId = null
            )
        }
    }

    fun confirmSelection() {
        val id = _uiState.value.selectedId ?: return
        val newBox = _uiState.value.newBoxes.firstOrNull { it.localId == id }
        if (newBox != null && newBox.name.isBlank()) {
            _uiState.update { it.copy(newBoxes = it.newBoxes.filterNot { b -> b.localId == id }) }
        }
        _uiState.update { it.copy(selectedId = null) }
    }

    fun save() {
        val state = _uiState.value
        val bitmap = state.captureData.bitmap ?: run {
            _saveState.value = SaveState.Error("No image to save")
            return
        }
        val groundTruth = buildGroundTruth(state)
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                goldenStore.save(
                    bitmap = bitmap,
                    name = name,
                    storageId = state.captureData.storageId,
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

    private fun buildGroundTruth(state: GoldenSaveUiState): List<GroundTruthItem> {
        val removedEntries = removedKnownItems.map { (itemId, itemName) ->
            GroundTruthItem(itemId = itemId, name = itemName, changeType = ItemChange.REMOVED)
        }
        val fillStateEntries = state.knownBoxes.mapNotNull { box ->
            box.fillState?.let { GroundTruthItem(itemId = box.itemId, name = box.name, changeType = it) }
        }
        val newEntries = state.newBoxes.filter { it.name.isNotBlank() }.map { box ->
            val centerX = box.boundingBox.x + box.boundingBox.width / 2f
            val centerY = box.boundingBox.y + box.boundingBox.height / 2f

            val cellName = resolveCellName(state.guideLines, 1, 1, 1, 1, centerX, centerY)
            GroundTruthItem(
                itemId = null,
                name = box.name,
                changeType = ItemChange.ADDED,
                cellName = cellName,
                box = box.boundingBox
            )
        }
        return removedEntries + fillStateEntries + newEntries
    }
}
