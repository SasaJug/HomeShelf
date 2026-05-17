package com.jugurdzija.homeshelf.ui.detail

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.GridCellEmbeddingStore
import com.jugurdzija.homeshelf.data.GridCellStore
import com.jugurdzija.homeshelf.data.GuideLineStore
import com.jugurdzija.homeshelf.embedding.GridCellEmbedder
import com.jugurdzija.homeshelf.homography.GridProcessor
import com.jugurdzija.homeshelf.util.mapLinesToImageCoords
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val guideLineStore: GuideLineStore,
    private val gridCellStore: GridCellStore,
    private val gridCellEmbeddingStore: GridCellEmbeddingStore,
    private val gridProcessor: GridProcessor,
    private val gridCellEmbedder: GridCellEmbedder
) : ViewModel() {

    val filePath: String = Uri.decode(savedStateHandle.get<String>("filePath") ?: "")

    val guideLines = mutableStateListOf<GuideLine>()
    var nextId by mutableIntStateOf(0)

    private val _processState = MutableStateFlow<GridProcessState>(GridProcessState.Idle)
    val processState: StateFlow<GridProcessState> = _processState

    init {
        if (filePath.isNotEmpty()) {
            viewModelScope.launch {
                val loaded = guideLineStore.load(filePath)
                guideLines.addAll(loaded)
                nextId = (loaded.maxOfOrNull { it.id } ?: -1) + 1
            }
        }
    }

    fun processGrid(canvasWidth: Int, canvasHeight: Int, bitmap: Bitmap) {
        viewModelScope.launch {
            _processState.value = GridProcessState.Processing
            try {
                guideLineStore.save(filePath, guideLines.toList())
                val (hPixels, vPixels) = mapLinesToImageCoords(
                    guideLines.toList(), canvasWidth, canvasHeight, bitmap.width, bitmap.height
                )
                val cells = gridProcessor.extract(bitmap, hPixels, vPixels)
                if (cells.isEmpty()) {
                    _processState.value = GridProcessState.NoCells
                } else {
                    gridCellStore.save(filePath, cells)
                    val embeddings = gridCellEmbedder.embed(cells)
                    gridCellEmbeddingStore.save(filePath, embeddings)
                    _processState.value = GridProcessState.Done(cells.size)
                }
            } catch (e: Exception) {
                _processState.value = GridProcessState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetProcessState() {
        _processState.value = GridProcessState.Idle
    }

}
