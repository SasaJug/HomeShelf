package com.jugurdzija.homeshelf.ui.compare

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.CaptureSettingsStore
import com.jugurdzija.homeshelf.data.GoldenStore
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.GuideLineStore
import com.jugurdzija.homeshelf.data.ReferenceImageStore
import com.jugurdzija.homeshelf.data.ReferenceItem
import com.jugurdzija.homeshelf.embedding.EmbedderOwner
import com.jugurdzija.homeshelf.ui.common.GUIDE_SIMILARITY_THRESHOLD
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class CompareViewModel @Inject constructor(
    private val store: ReferenceImageStore,
    private val embedder: EmbedderOwner,
    private val guideLineStore: GuideLineStore,
    private val settingsStore: CaptureSettingsStore,
    private val goldenStore: GoldenStore
) : ViewModel() {

    sealed interface CompareEvent {
        data object NavigateToSave : CompareEvent
    }

    private val _state = MutableStateFlow<CompareUiState>(CompareUiState.Loading)
    val state: StateFlow<CompareUiState> = _state.asStateFlow()

    private val _events = Channel<CompareEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var referencesWithBitmaps: List<Pair<ReferenceItem, Bitmap>> = emptyList()
    private var topReferenceItem: ReferenceItem? = null
    private val inferenceInFlight = AtomicBoolean(false)
    private var cachedGuideLines: Pair<String, List<GuideLine>>? = null
    private var framesAnalyzed = 0
    private var captureAttempt = 0

    init {
        viewModelScope.launch {
            val items = store.loadAll()
            if (items.isEmpty()) {
                _state.value = CompareUiState.MissingReference("No references saved. Go back and add some first.")
                return@launch
            }
            referencesWithBitmaps = items.mapNotNull { item -> store.decodeBitmap(item)?.let { item to it } }
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
                framesAnalyzed++
                val matches = embedder.embedAll(bitmap, referencesWithBitmaps)
                val top = matches.firstOrNull()
                val refItem = top?.let { referencesWithBitmaps.firstOrNull { r -> r.first.id == it.item.id }?.first }
                val guideLines = if (refItem != null && top.similarity >= GUIDE_SIMILARITY_THRESHOLD)
                    loadGuideLinesCached(refItem.file.absolutePath) else emptyList()

                if (top != null && top.similarity >= settingsStore.captureThreshold.value) {
                    topReferenceItem = refItem
                    _state.value = CompareUiState.CapturePending(matches, guideLines, framesAnalyzed, captureAttempt)
                    return@launch
                }
                _state.value = CompareUiState.Streaming(matches, guideLines)
            } finally {
                inferenceInFlight.set(false)
            }
        }
    }

    fun onScanAgain() {
        captureAttempt++
        framesAnalyzed = 0
        topReferenceItem = null
        _state.value = CompareUiState.Streaming(emptyList())
    }

    private suspend fun loadGuideLinesCached(filePath: String): List<GuideLine> {
        val cached = cachedGuideLines
        if (cached != null && cached.first == filePath) return cached.second
        val lines = guideLineStore.load(filePath)
        cachedGuideLines = filePath to lines
        return lines
    }

    fun onBitmapCaptured(bitmap: Bitmap) {
        val capturePending = _state.value as? CompareUiState.CapturePending ?: return
        val top = capturePending.matches.firstOrNull() ?: return
        viewModelScope.launch {
            goldenStore.populateHolder(
                bitmap = bitmap,
                referenceLabel = top.item.label,
                referenceFilePath = top.item.file.absolutePath,
                similarityScore = top.similarity,
                similarityThreshold = settingsStore.captureThreshold.value.toDouble(),
                allMatchScores = capturePending.matches.associate { it.item.label to it.similarity },
                framesAnalyzed = framesAnalyzed,
                captureAttempt = captureAttempt
            )
            _events.send(CompareEvent.NavigateToSave)
        }
    }

    fun onPermissionDenied() {
        _state.value = CompareUiState.PermissionDenied
    }
}
