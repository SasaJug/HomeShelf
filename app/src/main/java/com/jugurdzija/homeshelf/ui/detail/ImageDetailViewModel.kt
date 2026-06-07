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
import com.jugurdzija.homeshelf.data.ReferenceDataStore
import com.jugurdzija.homeshelf.usecase.ReferencePipeline
import com.jugurdzija.homeshelf.usecase.ReferencePipelineResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val referenceDataStore: ReferenceDataStore,
    private val referencePipeline: ReferencePipeline
) : ViewModel() {

    val filePath: String = Uri.decode(savedStateHandle.get<String>("filePath") ?: "")

    val guideLines = mutableStateListOf<GuideLine>()
    var nextId by mutableIntStateOf(0)

    private val _processState = MutableStateFlow<GridProcessState>(GridProcessState.Idle)
    val processState: StateFlow<GridProcessState> = _processState

    init {
        if (filePath.isNotEmpty()) {
            viewModelScope.launch {
                val loaded = referenceDataStore.load(filePath).guideLines
                guideLines.addAll(loaded)
                nextId = (loaded.maxOfOrNull { it.id } ?: -1) + 1
            }
        }
    }

    fun processGrid(canvasWidth: Int, canvasHeight: Int, bitmap: Bitmap) {
        viewModelScope.launch {
            _processState.value = GridProcessState.Processing
            _processState.value = when (
                val result = referencePipeline.run(bitmap, filePath, guideLines.toList(), canvasWidth, canvasHeight)
            ) {
                is ReferencePipelineResult.Done -> GridProcessState.Done(result.cellCount)
                is ReferencePipelineResult.NoCells -> GridProcessState.NoCells
                is ReferencePipelineResult.Error -> GridProcessState.Error(result.message)
            }
        }
    }

    fun resetProcessState() {
        _processState.value = GridProcessState.Idle
    }
}
