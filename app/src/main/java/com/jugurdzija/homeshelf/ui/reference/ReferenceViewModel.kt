package com.jugurdzija.homeshelf.ui.reference

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.StorageItem
import com.jugurdzija.homeshelf.data.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReferenceViewModel @Inject constructor(
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ReferenceListUiState>(ReferenceListUiState.Loading)
    val state: StateFlow<ReferenceListUiState> = _state.asStateFlow()

    private val _thumbnails = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val thumbnails: StateFlow<Map<String, Bitmap>> = _thumbnails.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            val items = storageRepository.loadAll()
            _state.value = if (items.isEmpty()) ReferenceListUiState.Empty
                           else ReferenceListUiState.Loaded(items)
            loadThumbnails(items)
        }
    }

    private fun loadThumbnails(items: List<StorageItem>) {
        viewModelScope.launch {
            items.filter { !_thumbnails.value.containsKey(it.id) }.forEach { item ->
                val bmp = storageRepository.decodeLatestBitmap(item.id, sampleSize = 4)
                if (bmp != null) _thumbnails.update { it + (item.id to bmp) }
            }
        }
    }

    fun onDelete(id: String) {
        viewModelScope.launch {
            storageRepository.delete(id)
            _thumbnails.update { it - id }
            reload()
        }
    }
}
