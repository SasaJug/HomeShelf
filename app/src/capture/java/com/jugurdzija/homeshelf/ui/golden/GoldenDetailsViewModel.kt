package com.jugurdzija.homeshelf.ui.golden

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.CaptureData
import com.jugurdzija.homeshelf.data.GoldenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLDecoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoldenDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val goldenStore: GoldenStore
) : ViewModel() {

    sealed interface State {
        data object Loading : State
        data object NotFound : State
        data class Loaded(val captureData: CaptureData) : State
    }

    private val name: String = URLDecoder.decode(checkNotNull(savedStateHandle["name"]), "UTF-8")

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val details = goldenStore.loadDetails(name)
            _state.value = details?.let { State.Loaded(it) } ?: State.NotFound
        }
    }
}
