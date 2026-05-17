package com.jugurdzija.homeshelf.ui.detail

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.GridCellEmbeddingStore
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.embedding.GridCellEmbedder
import com.jugurdzija.homeshelf.homography.GridProcessor
import com.jugurdzija.homeshelf.util.cosineSimilarity
import com.jugurdzija.homeshelf.util.mapLinesToImageCoords
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlignedDetailViewModel @Inject constructor(
    private val gridCellEmbeddingStore: GridCellEmbeddingStore,
    private val gridProcessor: GridProcessor,
    private val gridCellEmbedder: GridCellEmbedder
) : ViewModel() {

    private val _state = MutableStateFlow<AlignedDetailState>(AlignedDetailState.Idle)
    val state: StateFlow<AlignedDetailState> = _state.asStateFlow()

    fun analyzeGrid(canvasWidth: Int, canvasHeight: Int, bitmap: Bitmap, guideLines: List<GuideLine>) {
        if (_state.value !is AlignedDetailState.Idle) return
        val filePath = BitmapDetailHolder.pendingReferenceFilePath ?: run {
            _state.value = AlignedDetailState.NoReference
            return
        }
        viewModelScope.launch {
            _state.value = AlignedDetailState.Processing
            try {
                val refEmbeddings = gridCellEmbeddingStore.load(filePath)
                if (refEmbeddings.isEmpty()) {
                    _state.value = AlignedDetailState.NoReference
                    return@launch
                }
                val (hPixels, vPixels) = mapLinesToImageCoords(guideLines, canvasWidth, canvasHeight, bitmap.width, bitmap.height)
                val cells = gridProcessor.extract(bitmap, hPixels, vPixels)
                if (cells.isEmpty()) {
                    _state.value = AlignedDetailState.NoCells
                    return@launch
                }
                val embeddings = gridCellEmbedder.embed(cells)
                val similarities = embeddings.mapValues { (name, vec) ->
                    val refVec = refEmbeddings[name]
                    if (refVec != null) cosineSimilarity(vec, refVec) else 0f
                }
                _state.value = AlignedDetailState.Done(similarities)
            } catch (e: Exception) {
                _state.value = AlignedDetailState.Error(e.message ?: "Unknown error")
            }
        }
    }

}
