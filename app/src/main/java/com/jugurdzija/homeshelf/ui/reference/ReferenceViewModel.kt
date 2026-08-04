package com.jugurdzija.homeshelf.ui.reference

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.OnboardingPreferences
import com.jugurdzija.homeshelf.data.StorageItem
import com.jugurdzija.homeshelf.data.StorageListEntry
import com.jugurdzija.homeshelf.data.StorageRepository
import com.jugurdzija.homeshelf.data.calculateCompleteness
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReferenceViewModel @Inject constructor(
    private val storageRepository: StorageRepository,
    private val onboardingPreferences: OnboardingPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow<ReferenceListUiState>(ReferenceListUiState.Loading)
    val state: StateFlow<ReferenceListUiState> = _state.asStateFlow()

    private val _thumbnails = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val thumbnails: StateFlow<Map<String, Bitmap>> = _thumbnails.asStateFlow()

    private val _showIntroDialog = MutableStateFlow(!onboardingPreferences.hasSeenIntro())
    val showIntroDialog: StateFlow<Boolean> = _showIntroDialog.asStateFlow()

    init { reload() }

    fun dismissIntro() {
        onboardingPreferences.markIntroSeen()
        _showIntroDialog.value = false
    }

    fun reload() {
        viewModelScope.launch {
            val items = storageRepository.loadAll()
            if (items.isEmpty()) {
                _state.value = ReferenceListUiState.Empty
            } else {
                val entries = items.map { item ->
                    StorageListEntry(item, calculateCompleteness(storageRepository.loadLatestData(item.id)))
                }
                _state.value = ReferenceListUiState.Loaded(entries)
            }
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
}
