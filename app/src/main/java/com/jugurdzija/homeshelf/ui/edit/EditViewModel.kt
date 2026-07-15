package com.jugurdzija.homeshelf.ui.edit

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.PendingCaptureStore
import com.jugurdzija.homeshelf.data.StorageRepository
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

sealed interface EditNavEvent {
    data object Saved : EditNavEvent
    data object Discarded : EditNavEvent
}

@HiltViewModel
class EditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pendingCaptureStore: PendingCaptureStore,
    private val storageRepository: StorageRepository,
    private val storageSavePipeline: StorageSavePipeline
) : ViewModel() {

    val storageId: String? = savedStateHandle.get<String>("storageId")?.takeIf { it.isNotEmpty() }
    val isNewStorage: Boolean = storageId == null

    var name by mutableStateOf("")

    val guideLines = mutableStateListOf<GuideLine>()
    var nextId by mutableIntStateOf(0)

    private val _bitmapState = MutableStateFlow<Bitmap?>(null)
    val bitmapState: StateFlow<Bitmap?> = _bitmapState.asStateFlow()

    private val _saveState = MutableStateFlow<StorageSaveResult?>(null)
    val saveState: StateFlow<StorageSaveResult?> = _saveState.asStateFlow()

    private val _navEvent = MutableSharedFlow<EditNavEvent>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<EditNavEvent> = _navEvent

    init {
        viewModelScope.launch {
            val pending = pendingCaptureStore.load()
            _bitmapState.value = pending
                ?: if (storageId != null) {
                    storageRepository.decodeLatestBitmap(storageId)
                } else {
                    null
                }
        }
        if (storageId != null) {
            viewModelScope.launch {
                name = storageRepository.loadAll().firstOrNull { it.id == storageId }?.name ?: ""
                val loaded = storageRepository.loadLatestData(storageId).guideLines
                guideLines.addAll(loaded)
                nextId = (loaded.maxOfOrNull { it.id } ?: -1) + 1
            }
        }
    }

    fun save(canvasWidth: Int, canvasHeight: Int) {
        val bitmap = _bitmapState.value ?: return
        viewModelScope.launch {
            val result = storageSavePipeline.run(
                storageId, name, bitmap, guideLines.toList(), canvasWidth, canvasHeight
            )
            when (result) {
                is StorageSaveResult.Done -> {
                    pendingCaptureStore.clear()
                    _navEvent.emit(EditNavEvent.Saved)
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
            _navEvent.emit(EditNavEvent.Discarded)
        }
    }
}
