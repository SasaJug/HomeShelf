package com.jugurdzija.homeshelf.ui.reference

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.ReferenceImageStore
import com.jugurdzija.homeshelf.data.ReferenceItem
import com.jugurdzija.homeshelf.ui.common.decodeBitmapFromUri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReferenceViewModel @Inject constructor(
    private val store: ReferenceImageStore
) : ViewModel() {

    private val _state = MutableStateFlow<ReferenceListUiState>(ReferenceListUiState.Loading)
    val state: StateFlow<ReferenceListUiState> = _state.asStateFlow()

    private val _thumbnails = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val thumbnails: StateFlow<Map<String, Bitmap>> = _thumbnails.asStateFlow()

    init { reload() }

    private fun reload() {
        viewModelScope.launch {
            val items = store.loadAll()
            _state.value = if (items.isEmpty()) ReferenceListUiState.Empty
                           else ReferenceListUiState.Loaded(items)
            loadThumbnails(items)
        }
    }

    private fun loadThumbnails(items: List<ReferenceItem>) {
        viewModelScope.launch {
            items.filter { !_thumbnails.value.containsKey(it.id) }.forEach { item ->
                val bmp = store.decodeBitmap(item, sampleSize = 4)
                if (bmp != null) _thumbnails.update { it + (item.id to bmp) }
            }
        }
    }

    fun onCaptureSuccess(context: Context, uri: Uri) {
        viewModelScope.launch {
            val bitmap = decodeBitmapFromUri(context, uri) ?: run {
                val current = (_state.value as? ReferenceListUiState.Loaded)?.items ?: emptyList()
                _state.value = ReferenceListUiState.Error("Failed to decode image", current)
                return@launch
            }
            store.saveReference(bitmap)
            reload()
        }
    }

    fun onDelete(id: String) {
        viewModelScope.launch {
            store.delete(id)
            _thumbnails.update { it - id }
            reload()
        }
    }
}
