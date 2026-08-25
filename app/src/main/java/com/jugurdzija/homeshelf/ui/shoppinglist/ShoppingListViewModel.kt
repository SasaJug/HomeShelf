package com.jugurdzija.homeshelf.ui.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.ShoppingListRepository
import com.jugurdzija.homeshelf.stt.AudioRecorder
import com.jugurdzija.homeshelf.stt.SpeechToTextEngine
import com.jugurdzija.homeshelf.stt.VoiceInputState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository,
    private val audioRecorder: AudioRecorder,
    private val speechToTextEngine: SpeechToTextEngine
) : ViewModel() {

    private val _state = MutableStateFlow<ShoppingListUiState>(ShoppingListUiState.Loading)
    val state: StateFlow<ShoppingListUiState> = _state.asStateFlow()

    private val _newItemName = MutableStateFlow("")
    val newItemName: StateFlow<String> = _newItemName.asStateFlow()

    private val _voiceInputState = MutableStateFlow(VoiceInputState.IDLE)
    val voiceInputState: StateFlow<VoiceInputState> = _voiceInputState.asStateFlow()

    private val _voiceError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val voiceError: SharedFlow<String> = _voiceError

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            val items = shoppingListRepository.loadAll()
            _state.value = if (items.isEmpty()) ShoppingListUiState.Empty else ShoppingListUiState.Loaded(items)
        }
    }

    fun onNewItemNameChange(name: String) {
        _newItemName.value = name
    }

    fun onAdd() {
        val trimmed = _newItemName.value.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            shoppingListRepository.add(trimmed)
            _newItemName.value = ""
            reload()
        }
    }

    fun onRemove(id: String) {
        viewModelScope.launch {
            shoppingListRepository.remove(id)
            reload()
        }
    }

    fun startVoiceInput() {
        if (_voiceInputState.value != VoiceInputState.IDLE) return
        _voiceInputState.value = VoiceInputState.RECORDING
        audioRecorder.start()
    }

    fun stopVoiceInput() {
        if (_voiceInputState.value != VoiceInputState.RECORDING) return
        _voiceInputState.value = VoiceInputState.PROCESSING
        viewModelScope.launch {
            val samples = audioRecorder.stop()
            speechToTextEngine.transcribe(samples)
                .onSuccess { text ->
                    if (text.isBlank()) {
                        _voiceError.tryEmit("Didn't understand that — try again")
                    } else {
                        _newItemName.value = text
                    }
                }
                .onFailure { e ->
                    _voiceError.tryEmit(e.message ?: "Voice input failed")
                }
            _voiceInputState.value = VoiceInputState.IDLE
        }
    }
}
