package com.jugurdzija.homeshelf.ui.confirm

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.PendingCaptureStore
import com.jugurdzija.homeshelf.data.StorageRepository
import com.jugurdzija.homeshelf.ui.nav.Routes
import com.jugurdzija.homeshelf.usecase.StorageSavePipeline
import com.jugurdzija.homeshelf.usecase.StorageSaveResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ConfirmCaptureNavEvent {
    data class Saved(val storageId: String) : ConfirmCaptureNavEvent
    data object Discarded : ConfirmCaptureNavEvent
}

@HiltViewModel
class ConfirmCaptureViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pendingCaptureStore: PendingCaptureStore,
    private val storageRepository: StorageRepository,
    private val storageSavePipeline: StorageSavePipeline
) : ViewModel() {

    val storageId: String? = savedStateHandle.get<String>(Routes.ARG_STORAGE_ID)?.takeIf { it.isNotEmpty() }
    val isRescan: Boolean = storageId != null

    var name by mutableStateOf("")

    private val _bitmapState = MutableStateFlow<Bitmap?>(null)
    val bitmapState: StateFlow<Bitmap?> = _bitmapState.asStateFlow()

    private val _existingName = MutableStateFlow<String?>(null)
    val existingName: StateFlow<String?> = _existingName.asStateFlow()

    private val _saveState = MutableStateFlow<StorageSaveResult?>(null)
    val saveState: StateFlow<StorageSaveResult?> = _saveState.asStateFlow()

    private val _navEvent = MutableSharedFlow<ConfirmCaptureNavEvent>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<ConfirmCaptureNavEvent> = _navEvent

    init {
        viewModelScope.launch {
            _bitmapState.value = pendingCaptureStore.load()
        }
        if (storageId != null) {
            viewModelScope.launch {
                _existingName.value = storageRepository.loadAll().firstOrNull { it.id == storageId }?.name
            }
        }
    }

    fun save() {
        val bitmap = _bitmapState.value ?: return
        val storageName = if (isRescan) existingName.value.orEmpty() else name.trim()
        if (!isRescan && storageName.isBlank()) return
        viewModelScope.launch {
            val result = storageSavePipeline.run(
                storageId = storageId,
                name = storageName,
                bitmap = bitmap,
                guideLines = emptyList(),
                canvasWidth = bitmap.width,
                canvasHeight = bitmap.height,
                resolvedMarkedItems = emptyList()
            )
            when (result) {
                is StorageSaveResult.Done -> {
                    pendingCaptureStore.clear()
                    _navEvent.emit(ConfirmCaptureNavEvent.Saved(result.storageId))
                }
                is StorageSaveResult.Error -> {
                    _saveState.value = result
                }
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = null
    }

    fun discard() {
        viewModelScope.launch {
            pendingCaptureStore.clear()
            _navEvent.emit(ConfirmCaptureNavEvent.Discarded)
        }
    }
}
