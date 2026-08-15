package com.jugurdzija.homeshelf.ui.golden

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.GoldenStore
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.StorageRepository
import com.jugurdzija.homeshelf.ui.nav.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoldenCaptureViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val storageRepository: StorageRepository,
    private val goldenStore: GoldenStore
) : ViewModel() {

    sealed interface CaptureReadinessState {
        data object Loading : CaptureReadinessState
        data class Ready(val guideLines: List<GuideLine>) : CaptureReadinessState
        data class Unavailable(val reason: String) : CaptureReadinessState
    }

    val storageId: String = checkNotNull(savedStateHandle[Routes.ARG_STORAGE_ID])

    private val _guideLineState = MutableStateFlow<CaptureReadinessState>(CaptureReadinessState.Loading)
    val guideLineState: StateFlow<CaptureReadinessState> = _guideLineState.asStateFlow()

    private val _navigateToSave = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToSave: SharedFlow<Unit> = _navigateToSave

    private var referenceLabel: String = ""

    init {
        viewModelScope.launch {
            referenceLabel = storageRepository.loadAll().firstOrNull { it.id == storageId }?.name ?: ""
            val data = storageRepository.loadLatestData(storageId)
            val hasGuideLines = data.guideLines.size >= 4
            val hasMarkedItems = data.markedItems.isNotEmpty()
            _guideLineState.value = when {
                hasGuideLines && hasMarkedItems -> CaptureReadinessState.Ready(data.guideLines)
                !hasMarkedItems && !hasGuideLines -> CaptureReadinessState.Unavailable(
                    "This storage has no grid or marked items yet. Add both before capturing a comparison photo."
                )
                !hasMarkedItems -> CaptureReadinessState.Unavailable(
                    "This storage has no marked items yet. Mark items before capturing a comparison photo."
                )
                else -> CaptureReadinessState.Unavailable(
                    "This storage has no guide lines set up yet. Add guide lines before capturing a comparison photo."
                )
            }
        }
    }

    fun onCaptureBitmap(bitmap: Bitmap) {
        if (_guideLineState.value !is CaptureReadinessState.Ready) return
        viewModelScope.launch {
            goldenStore.populateHolder(bitmap, storageId, referenceLabel)
            _navigateToSave.emit(Unit)
        }
    }
}
