package com.jugurdzija.homeshelf.ui.reference

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.domain.model.StorageListEntry
import com.jugurdzija.homeshelf.domain.usecases.getstorageoverview.GetStorageOverviewUseCase
import com.jugurdzija.homeshelf.domain.usecases.getstoragereference.GetStorageReferenceUseCase
import com.jugurdzija.homeshelf.domain.usecases.introseen.IntroSeenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReferenceViewModel @Inject constructor(
    private val getStorageOverviewUseCase: GetStorageOverviewUseCase,
    private val getStorageReferenceUseCase: GetStorageReferenceUseCase,
    private val introSeenUseCase: IntroSeenUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<ReferenceListUiState>(ReferenceListUiState.Loading)
    val state: StateFlow<ReferenceListUiState> = _state.asStateFlow()

    private val _thumbnails = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val thumbnails: StateFlow<Map<String, Bitmap>> = _thumbnails.asStateFlow()

    private val _showIntroDialog = MutableStateFlow(!introSeenUseCase.hasSeenIntro())
    val showIntroDialog: StateFlow<Boolean> = _showIntroDialog.asStateFlow()

    init { reload() }

    fun dismissIntro() {
        introSeenUseCase.markIntroSeen()
        _showIntroDialog.value = false
    }

    fun reload() {
        viewModelScope.launch {
            val entries = getStorageOverviewUseCase.getOverview()
            _state.value = if (entries.isEmpty()) ReferenceListUiState.Empty else ReferenceListUiState.Loaded(entries)
            loadThumbnails(entries)
        }
    }

    private fun loadThumbnails(entries: List<StorageListEntry>) {
        viewModelScope.launch {
            entries.map { it.item }.filter { !_thumbnails.value.containsKey(it.id) }.forEach { item ->
                val bmp = getStorageReferenceUseCase.getThumbnail(item.id)
                if (bmp != null) _thumbnails.update { it + (item.id to bmp) }
            }
        }
    }
}
