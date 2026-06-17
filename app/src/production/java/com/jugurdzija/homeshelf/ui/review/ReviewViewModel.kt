package com.jugurdzija.homeshelf.ui.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.PendingCaptureStore
import com.jugurdzija.homeshelf.data.StorageStore
import com.jugurdzija.homeshelf.usecase.ComparisonPipeline
import com.jugurdzija.homeshelf.usecase.ComparisonResult
import com.jugurdzija.homeshelf.usecase.StorageSavePipeline
import com.jugurdzija.homeshelf.usecase.StorageSaveResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ReviewNavEvent {
    data object ToReference : ReviewNavEvent
    data class ToEdit(val storageId: String) : ReviewNavEvent
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pendingCaptureStore: PendingCaptureStore,
    private val storageStore: StorageStore,
    private val comparisonPipeline: ComparisonPipeline,
    private val storageSavePipeline: StorageSavePipeline
) : ViewModel() {

    val storageId: String = checkNotNull(savedStateHandle["storageId"])

    private val _state = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    private val _saveState = MutableStateFlow<StorageSaveResult?>(null)
    val saveState: StateFlow<StorageSaveResult?> = _saveState.asStateFlow()

    private val _navEvent = MutableSharedFlow<ReviewNavEvent>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<ReviewNavEvent> = _navEvent

    init {
        viewModelScope.launch {
            val storageName = storageStore.loadAll().firstOrNull { it.id == storageId }?.name ?: ""
            val pending = pendingCaptureStore.load()
            if (pending == null) {
                _state.value = ReviewUiState.CompareError(storageName, "No captured image found")
                return@launch
            }
            _state.value = when (val result = comparisonPipeline.run(pending, storageId)) {
                is ComparisonResult.Success -> ReviewUiState.Done(
                    storageName = storageName,
                    alignedBitmap = result.alignedBitmap,
                    guideLines = result.guideLines,
                    similarities = result.similarities
                )
                ComparisonResult.AlignmentFailed -> ReviewUiState.CompareError(storageName, "Alignment failed — try capturing again")
                ComparisonResult.NoGuideLines -> ReviewUiState.CompareError(storageName, "No guide lines saved for this storage")
                ComparisonResult.NoEmbeddings -> ReviewUiState.CompareError(storageName, "No reference data saved for this storage")
                ComparisonResult.NoCells -> ReviewUiState.CompareError(storageName, "Could not extract grid cells")
            }
        }
    }

    fun save() {
        val done = _state.value as? ReviewUiState.Done ?: return
        viewModelScope.launch {
            val result = storageSavePipeline.run(
                storageId,
                done.storageName,
                done.alignedBitmap,
                done.guideLines,
                done.alignedBitmap.width,
                done.alignedBitmap.height
            )
            when (result) {
                is StorageSaveResult.Done -> {
                    pendingCaptureStore.clear()
                    _navEvent.emit(ReviewNavEvent.ToReference)
                }
                is StorageSaveResult.Error -> _saveState.value = result
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = null
    }

    fun discard() {
        viewModelScope.launch {
            pendingCaptureStore.clear()
            _navEvent.emit(ReviewNavEvent.ToReference)
        }
    }

    fun navigateToEdit() {
        val done = _state.value as? ReviewUiState.Done ?: return
        viewModelScope.launch {
            pendingCaptureStore.save(done.alignedBitmap)
            _navEvent.emit(ReviewNavEvent.ToEdit(storageId))
        }
    }
}
