package com.jugurdzija.homeshelf.ui.scan

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.PendingCaptureStore
import com.jugurdzija.homeshelf.data.StorageItem
import com.jugurdzija.homeshelf.data.StorageRepository
import com.jugurdzija.homeshelf.embedding.EmbedderOwner
import com.jugurdzija.homeshelf.ui.common.CAPTURE_SIMILARITY_THRESHOLD
import com.jugurdzija.homeshelf.ui.nav.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

sealed interface ScanNavEvent {
    data class ToReview(val storageId: String) : ScanNavEvent
    data class ToConfirm(val storageId: String?) : ScanNavEvent
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val storageRepository: StorageRepository,
    private val embedder: EmbedderOwner,
    private val pendingCaptureStore: PendingCaptureStore
) : ViewModel() {

    private val pinnedStorageId: String? = savedStateHandle.get<String>(Routes.ARG_STORAGE_ID)?.takeIf { it.isNotEmpty() }
    private val isRescan: Boolean = savedStateHandle.get<String>(Routes.ARG_MODE) == Routes.MODE_RESCAN

    private val _state = MutableStateFlow<ScanUiState>(ScanUiState.Loading)
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private val _navEvent = MutableSharedFlow<ScanNavEvent>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<ScanNavEvent> = _navEvent

    private var storagesWithBitmaps: List<Pair<StorageItem, Bitmap>> = emptyList()
    private val inferenceInFlight = AtomicBoolean(false)
    private var cachedGuideLines: Pair<String, List<GuideLine>>? = null

    init {
        val pinnedId = pinnedStorageId
        if (pinnedId != null) {
            viewModelScope.launch {
                val item = storageRepository.loadAll().firstOrNull { it.id == pinnedId }
                val guideLines = loadGuideLinesCached(pinnedId)
                _state.value = ScanUiState.Streaming(detected = item, guideLines = guideLines)
            }
        } else {
            viewModelScope.launch {
                val items = storageRepository.loadAll()
                storagesWithBitmaps = items.mapNotNull { item ->
                    storageRepository.decodeLatestBitmap(item.id)?.let { item to it }
                }
                _state.value = ScanUiState.Streaming()
            }
        }
        viewModelScope.launch {
            embedder.errors.collect { msg ->
                val current = _state.value
                val detected = (current as? ScanUiState.Streaming)?.detected
                    ?: (current as? ScanUiState.Error)?.detected
                val guideLines = (current as? ScanUiState.Streaming)?.guideLines
                    ?: (current as? ScanUiState.Error)?.guideLines
                    ?: emptyList()
                _state.value = ScanUiState.Error(msg, detected, guideLines)
            }
        }
    }

    fun onFrameReceived(bitmap: Bitmap) {
        if (pinnedStorageId != null) return
        val s = _state.value
        if (s !is ScanUiState.Streaming && s !is ScanUiState.Error) return
        if (!inferenceInFlight.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                if (storagesWithBitmaps.isEmpty()) {
                    _state.value = ScanUiState.Streaming()
                    return@launch
                }
                val matches = embedder.embedAll(bitmap, storagesWithBitmaps)
                val top = matches.firstOrNull()
                if (top != null && top.similarity >= CAPTURE_SIMILARITY_THRESHOLD) {
                    val detected = top.item
                    val guideLines = loadGuideLinesCached(detected.id)
                    _state.value = ScanUiState.Streaming(matches, detected, guideLines)
                } else {
                    _state.value = ScanUiState.Streaming(matches, null, emptyList())
                }
            } finally {
                inferenceInFlight.set(false)
            }
        }
    }

    private suspend fun loadGuideLinesCached(storageId: String): List<GuideLine> {
        val cached = cachedGuideLines
        if (cached != null && cached.first == storageId) return cached.second
        val lines = storageRepository.loadLatestData(storageId).guideLines
        cachedGuideLines = storageId to lines
        return lines
    }

    fun onCaptureBitmap(bitmap: Bitmap) {
        val pinnedId = pinnedStorageId
        val detected = (_state.value as? ScanUiState.Streaming)?.detected
        viewModelScope.launch {
            pendingCaptureStore.save(bitmap)
            when {
                pinnedId != null && isRescan -> _navEvent.emit(ScanNavEvent.ToConfirm(pinnedId))
                pinnedId != null -> _navEvent.emit(ScanNavEvent.ToReview(pinnedId))
                detected != null -> _navEvent.emit(ScanNavEvent.ToReview(detected.id))
                else -> _navEvent.emit(ScanNavEvent.ToConfirm(null))
            }
        }
    }

    fun onPermissionDenied() {
        _state.value = ScanUiState.Error("Camera permission is required", null, emptyList())
    }
}
