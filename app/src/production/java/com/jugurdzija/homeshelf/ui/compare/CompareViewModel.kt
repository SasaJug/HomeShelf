package com.jugurdzija.homeshelf.ui.compare

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.GuideLineStore
import com.jugurdzija.homeshelf.data.ReferenceImageStore
import com.jugurdzija.homeshelf.data.ReferenceItem
import com.jugurdzija.homeshelf.embedding.EmbedderOwner
import com.jugurdzija.homeshelf.ui.common.CAPTURE_SIMILARITY_THRESHOLD
import com.jugurdzija.homeshelf.ui.common.GUIDE_SIMILARITY_THRESHOLD
import com.jugurdzija.homeshelf.ui.detail.BitmapDetailHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class CompareViewModel @Inject constructor(
    private val store: ReferenceImageStore,
    private val embedder: EmbedderOwner,
    private val guideLineStore: GuideLineStore
) : ViewModel() {

    private val _state = MutableStateFlow<CompareUiState>(CompareUiState.Loading)
    val state: StateFlow<CompareUiState> = _state.asStateFlow()

    private var referencesWithBitmaps: List<Pair<ReferenceItem, Bitmap>> = emptyList()
    private var topReferenceItem: ReferenceItem? = null
    private val inferenceInFlight = AtomicBoolean(false)
    private var cachedGuideLines: Pair<String, List<GuideLine>>? = null

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
                val refItem = if (top != null) referencesWithBitmaps.firstOrNull { it.first.id == top.item.id }?.first else null
                val guideLines = if (refItem != null && top != null && top.similarity >= GUIDE_SIMILARITY_THRESHOLD)
                    loadGuideLinesCached(refItem.file.absolutePath) else emptyList()

                if (top != null && top.similarity >= CAPTURE_SIMILARITY_THRESHOLD) {
                    topReferenceItem = refItem
                    _state.value = CompareUiState.CapturePending(matches, guideLines)
                    return@launch
                }
                _state.value = CompareUiState.Streaming(matches, guideLines)
            } finally {
                inferenceInFlight.set(false)
            }
        }
    }

    fun onPreviewBitmapCaptured(previewBitmap: Bitmap) {
        val s = _state.value as? CompareUiState.CapturePending ?: return
        val refItem = topReferenceItem ?: return
        val referenceBitmap = referencesWithBitmaps.firstOrNull { it.first.id == refItem.id }?.second ?: return

        BitmapDetailHolder.capturedBitmap = previewBitmap
        BitmapDetailHolder.referenceBitmap = referenceBitmap
        BitmapDetailHolder.referenceFilePath = refItem.file.absolutePath

        val guideLines = cachedGuideLines?.takeIf { it.first == refItem.file.absolutePath }?.second ?: s.guideLines
        _state.value = CompareUiState.Aligned(previewBitmap, s.matches, guideLines)
    }

    private suspend fun loadGuideLinesCached(filePath: String): List<GuideLine> {
        val cached = cachedGuideLines
        if (cached != null && cached.first == filePath) return cached.second
        val lines = guideLineStore.load(filePath)
        cachedGuideLines = filePath to lines
        return lines
    }

    fun onScanAgain() {
        topReferenceItem = null
        _state.value = CompareUiState.Streaming(emptyList())
    }

    fun onPermissionDenied() {
        _state.value = CompareUiState.PermissionDenied
    }
}
