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

    sealed interface GuideLineState {
        data object Loading : GuideLineState
        data class Ready(val guideLines: List<GuideLine>) : GuideLineState
        data object Unavailable : GuideLineState
    }

    val storageId: String = checkNotNull(savedStateHandle[Routes.ARG_STORAGE_ID])

    private val _guideLineState = MutableStateFlow<GuideLineState>(GuideLineState.Loading)
    val guideLineState: StateFlow<GuideLineState> = _guideLineState.asStateFlow()

    private val _navigateToSave = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToSave: SharedFlow<Unit> = _navigateToSave

    private var referenceLabel: String = ""

    init {
        viewModelScope.launch {
            referenceLabel = storageRepository.loadAll().firstOrNull { it.id == storageId }?.name ?: ""
            val lines = storageRepository.loadLatestData(storageId).guideLines
            _guideLineState.value = if (lines.size >= 4) GuideLineState.Ready(lines) else GuideLineState.Unavailable
        }
    }

    fun onCaptureBitmap(bitmap: Bitmap) {
        if (_guideLineState.value !is GuideLineState.Ready) return
        viewModelScope.launch {
            goldenStore.populateHolder(bitmap, storageId, referenceLabel)
            _navigateToSave.emit(Unit)
        }
    }
}
