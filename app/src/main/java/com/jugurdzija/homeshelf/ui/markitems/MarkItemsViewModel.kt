package com.jugurdzija.homeshelf.ui.markitems

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.BoundingBox
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.MarkedItem
import com.jugurdzija.homeshelf.data.StorageRepository
import com.jugurdzija.homeshelf.llm.ItemDetector
import com.jugurdzija.homeshelf.ui.nav.Routes
import com.jugurdzija.homeshelf.util.mapLinesToImageCoords
import com.jugurdzija.homeshelf.util.resolveCellName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface DetectState {
    data object Loading : DetectState
    data class Error(val message: String) : DetectState
}

@HiltViewModel
class MarkItemsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val storageRepository: StorageRepository,
    private val itemDetector: ItemDetector
) : ViewModel() {

    val storageId: String = checkNotNull(savedStateHandle[Routes.ARG_STORAGE_ID])

    var storageName by mutableStateOf("")
        private set

    private val _bitmapState = MutableStateFlow<Bitmap?>(null)
    val bitmapState: StateFlow<Bitmap?> = _bitmapState.asStateFlow()

    val guideLines = mutableStateListOf<GuideLine>()
    val markedItems = mutableStateListOf<MarkedItem>()

    var selectedId by mutableStateOf<String?>(null)
        private set

    private val _detectState = MutableStateFlow<DetectState?>(null)
    val detectState: StateFlow<DetectState?> = _detectState.asStateFlow()

    init {
        viewModelScope.launch {
            storageName = storageRepository.loadAll().firstOrNull { it.id == storageId }?.name ?: ""
            _bitmapState.value = storageRepository.decodeLatestBitmap(storageId)
            val data = storageRepository.loadLatestData(storageId)
            guideLines.addAll(data.guideLines)
            markedItems.addAll(data.markedItems)
        }
    }

    fun createItem(box: BoundingBox) {
        val item = MarkedItem(id = UUID.randomUUID().toString(), name = "", boundingBox = box)
        markedItems.add(item)
        selectedId = item.id
    }

    fun select(id: String) {
        selectedId = id
    }

    fun updateBoundingBox(id: String, box: BoundingBox) {
        val idx = markedItems.indexOfFirst { it.id == id }
        if (idx >= 0) markedItems[idx] = markedItems[idx].copy(boundingBox = box)
    }

    fun updateName(id: String, name: String) {
        val idx = markedItems.indexOfFirst { it.id == id }
        if (idx >= 0) markedItems[idx] = markedItems[idx].copy(name = name)
    }

    fun updateTransparent(id: String, isTransparentContainer: Boolean) {
        val idx = markedItems.indexOfFirst { it.id == id }
        if (idx >= 0) markedItems[idx] = markedItems[idx].copy(isTransparentContainer = isTransparentContainer)
    }

    fun confirmSelection(canvasWidth: Int, canvasHeight: Int) {
        val id = selectedId ?: return
        val idx = markedItems.indexOfFirst { it.id == id }
        if (idx < 0) {
            selectedId = null
            return
        }
        val item = markedItems[idx]
        if (item.name.isBlank()) {
            markedItems.removeAt(idx)
        } else {
            markedItems[idx] = item.copy(cellName = resolveCellNameFor(canvasWidth, canvasHeight, item.boundingBox))
        }
        selectedId = null
        persist()
    }

    fun deleteItem(id: String) {
        markedItems.removeAll { it.id == id }
        if (selectedId == id) selectedId = null
        persist()
    }

    private fun resolveCellNameFor(canvasWidth: Int, canvasHeight: Int, box: BoundingBox): String? {
        val bitmap = _bitmapState.value ?: return null
        return resolveCellName(
            guideLines, canvasWidth, canvasHeight, bitmap.width, bitmap.height,
            box.x + box.width / 2f, box.y + box.height / 2f
        )
    }

    fun runAiDetection(canvasWidth: Int, canvasHeight: Int) {
        val bitmap = _bitmapState.value ?: return
        if (_detectState.value is DetectState.Loading) return
        _detectState.value = DetectState.Loading
        viewModelScope.launch {
            val (hPixels, vPixels) = mapLinesToImageCoords(guideLines, canvasWidth, canvasHeight, bitmap.width, bitmap.height)
            val cropRect = if (hPixels.size >= 2 && vPixels.size >= 2) {
                Rect(
                    vPixels.first().toInt().coerceIn(0, bitmap.width - 1),
                    hPixels.first().toInt().coerceIn(0, bitmap.height - 1),
                    vPixels.last().toInt().coerceIn(1, bitmap.width),
                    hPixels.last().toInt().coerceIn(1, bitmap.height)
                )
            } else {
                null
            }
            val target = cropRect?.let {
                Bitmap.createBitmap(bitmap, it.left, it.top, it.width(), it.height())
            } ?: bitmap

            itemDetector.detect(target)
                .onSuccess { detected ->
                    markedItems.clear()
                    selectedId = null
                    detected.forEach { detectedItem ->
                        val box = if (cropRect != null) {
                            BoundingBox(
                                x = (cropRect.left + detectedItem.box.x * cropRect.width()) / bitmap.width.toFloat(),
                                y = (cropRect.top + detectedItem.box.y * cropRect.height()) / bitmap.height.toFloat(),
                                width = detectedItem.box.width * cropRect.width() / bitmap.width.toFloat(),
                                height = detectedItem.box.height * cropRect.height() / bitmap.height.toFloat()
                            )
                        } else {
                            detectedItem.box
                        }
                        markedItems.add(
                            MarkedItem(
                                id = UUID.randomUUID().toString(),
                                name = detectedItem.name,
                                boundingBox = box,
                                cellName = resolveCellNameFor(canvasWidth, canvasHeight, box),
                                isTransparentContainer = detectedItem.isTransparentContainer
                            )
                        )
                    }
                    persist()
                    _detectState.value = null
                }
                .onFailure { e ->
                    _detectState.value = DetectState.Error(e.message ?: "Couldn't detect items")
                }
        }
    }

    fun resetDetectState() {
        _detectState.value = null
    }

    private fun persist() {
        val snapshot = markedItems.toList()
        viewModelScope.launch { storageRepository.saveMarkedItems(storageId, snapshot) }
    }
}
