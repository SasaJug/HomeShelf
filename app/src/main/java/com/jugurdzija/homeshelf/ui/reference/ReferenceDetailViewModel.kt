package com.jugurdzija.homeshelf.ui.reference

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.StorageRepository
import com.jugurdzija.homeshelf.ui.nav.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ReferenceDetailUiState {
    data object Loading : ReferenceDetailUiState
    data class Loaded(
        val storageId: String,
        val storageName: String,
        val bitmap: Bitmap,
        val hasGuideLines: Boolean,
        val hasMarkedItems: Boolean
    ) : ReferenceDetailUiState
    data class Error(val message: String) : ReferenceDetailUiState
}

sealed interface ReferenceDetailNavEvent {
    data object Deleted : ReferenceDetailNavEvent
}

@HiltViewModel
class ReferenceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val storageId: String = requireNotNull(savedStateHandle.get<String>(Routes.ARG_STORAGE_ID))

    private val _state = MutableStateFlow<ReferenceDetailUiState>(ReferenceDetailUiState.Loading)
    val state: StateFlow<ReferenceDetailUiState> = _state.asStateFlow()

    private val _showDeleteConfirmation = MutableStateFlow(false)
    val showDeleteConfirmation: StateFlow<Boolean> = _showDeleteConfirmation.asStateFlow()

    private val _navEvent = MutableSharedFlow<ReferenceDetailNavEvent>()
    val navEvent: SharedFlow<ReferenceDetailNavEvent> = _navEvent

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val item = storageRepository.loadAll().firstOrNull { it.id == storageId }
            val bitmap = storageRepository.decodeLatestBitmap(storageId)
            _state.value = when {
                item == null -> ReferenceDetailUiState.Error("Storage not found")
                bitmap == null -> ReferenceDetailUiState.Error("No reference photo available")
                else -> {
                    val data = storageRepository.loadLatestData(storageId)
                    ReferenceDetailUiState.Loaded(
                        storageId = storageId,
                        storageName = item.name,
                        bitmap = bitmap,
                        hasGuideLines = data.guideLines.isNotEmpty(),
                        hasMarkedItems = data.markedItems.isNotEmpty()
                    )
                }
            }
        }
    }

    fun requestDelete() {
        _showDeleteConfirmation.value = true
    }

    fun cancelDelete() {
        _showDeleteConfirmation.value = false
    }

    fun confirmDelete() {
        _showDeleteConfirmation.value = false
        viewModelScope.launch {
            storageRepository.delete(storageId)
            _navEvent.emit(ReferenceDetailNavEvent.Deleted)
        }
    }

    fun renameStorage(newName: String) {
        val current = _state.value
        if (current !is ReferenceDetailUiState.Loaded) return
        val trimmed = newName.trim()
        if (trimmed.isBlank() || trimmed == current.storageName) return
        viewModelScope.launch {
            storageRepository.rename(storageId, trimmed)
            _state.value = current.copy(storageName = trimmed)
        }
    }
}
