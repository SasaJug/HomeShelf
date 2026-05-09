package com.jugurdzija.homeshelf.ui.compare

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.ReferenceImageStore
import com.jugurdzija.homeshelf.data.ReferenceItem
import com.jugurdzija.homeshelf.embedding.EmbedderOwner
import com.jugurdzija.homeshelf.homography.HomographyProcessor
import com.jugurdzija.homeshelf.ui.common.CAPTURE_SIMILARITY_THRESHOLD
import com.jugurdzija.homeshelf.ui.common.GUIDE_SIMILARITY_THRESHOLD
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class CompareViewModel @Inject constructor(
    private val store: ReferenceImageStore,
    private val embedder: EmbedderOwner
) : ViewModel() {

    private val _state = MutableStateFlow<CompareUiState>(CompareUiState.Loading)
    val state: StateFlow<CompareUiState> = _state.asStateFlow()

    private var referencesWithBitmaps: List<Pair<ReferenceItem, Bitmap>> = emptyList()
    private val inferenceInFlight = AtomicBoolean(false)

    init {
        viewModelScope.launch {
            val items = store.loadAll()
            if (items.isEmpty()) {
                _state.value = CompareUiState.MissingReference("No references saved. Go back and add some first.")
                return@launch
            }
            referencesWithBitmaps = items.mapNotNull { item ->
                store.decodeBitmap(item)?.let { item to it }
            }
            if (referencesWithBitmaps.isEmpty()) {
                _state.value = CompareUiState.MissingReference("Could not load reference images.")
                return@launch
            }
            _state.value = CompareUiState.Streaming(emptyList())
        }
        viewModelScope.launch {
            embedder.errors.collect { msg ->
                val currentMatches = (_state.value as? CompareUiState.Streaming)?.matches
                    ?: (_state.value as? CompareUiState.Error)?.matches
                    ?: emptyList()
                _state.value = CompareUiState.Error(msg, currentMatches)
            }
        }
    }

    fun onFrameReceived(bitmap: Bitmap) {
        val s = _state.value
        if (s !is CompareUiState.Streaming && s !is CompareUiState.Error) return
        if (!inferenceInFlight.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                val matches = embedder.embedAll(bitmap, referencesWithBitmaps)
                val top = matches.firstOrNull()
                if (top != null && top.similarity >= CAPTURE_SIMILARITY_THRESHOLD) {
                    _state.value = CompareUiState.Captured(bitmap, matches)
                    return@launch
                }
                val guideBitmap = if (top != null && top.similarity >= GUIDE_SIMILARITY_THRESHOLD) {
                    referencesWithBitmaps.firstOrNull { it.first.id == top.item.id }?.second
                } else null
                _state.value = CompareUiState.Streaming(matches, guideBitmap)
            } finally {
                inferenceInFlight.set(false)
            }
        }
    }

    fun onCapturedFrame(frozenBitmap: Bitmap) {
        val current = _state.value as? CompareUiState.Captured ?: return
        val topMatch = current.matches.firstOrNull() ?: return
        val referenceBitmap = referencesWithBitmaps.firstOrNull { it.first.id == topMatch.item.id }?.second ?: return

        viewModelScope.launch {
            val aligned = withContext(Dispatchers.Default) {
                HomographyProcessor.align(frozenBitmap, referenceBitmap)
            }
            if (aligned != null) {
                _state.value = CompareUiState.Aligned(aligned, referenceBitmap, current.matches)
            } else {
                _state.value = CompareUiState.Error("Could not align — try holding the camera steady", current.matches)
            }
        }
    }

    fun onScanAgain() {
        _state.value = CompareUiState.Streaming(emptyList())
    }

    fun onPermissionDenied() {
        _state.value = CompareUiState.PermissionDenied
    }
}
