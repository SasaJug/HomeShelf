package com.jugurdzija.homeshelf.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.usecase.ComparisonPipeline
import com.jugurdzija.homeshelf.usecase.ComparisonResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlignedDetailViewModel @Inject constructor(
    private val pipeline: ComparisonPipeline
) : ViewModel() {

    private val _state = MutableStateFlow<AlignedDetailState>(AlignedDetailState.Idle)
    val state: StateFlow<AlignedDetailState> = _state.asStateFlow()

    fun analyze() {
        if (_state.value !is AlignedDetailState.Idle) return
        val captured = BitmapDetailHolder.capturedBitmap ?: run {
            _state.value = AlignedDetailState.NoReference
            return
        }
        val reference = BitmapDetailHolder.referenceBitmap ?: run {
            _state.value = AlignedDetailState.NoReference
            return
        }
        val filePath = BitmapDetailHolder.referenceFilePath ?: run {
            _state.value = AlignedDetailState.NoReference
            return
        }
        viewModelScope.launch {
            _state.value = AlignedDetailState.Processing
            try {
                _state.value = when (val result = pipeline.run(captured, reference, filePath)) {
                    is ComparisonResult.Success -> AlignedDetailState.Done(
                        result.alignedBitmap,
                        result.guideLines,
                        result.similarities
                    )
                    ComparisonResult.AlignmentFailed -> AlignedDetailState.AlignmentFailed
                    ComparisonResult.NoGuideLines, ComparisonResult.NoEmbeddings -> AlignedDetailState.NoReference
                    ComparisonResult.NoCells -> AlignedDetailState.NoCells
                }
            } catch (e: Exception) {
                _state.value = AlignedDetailState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
