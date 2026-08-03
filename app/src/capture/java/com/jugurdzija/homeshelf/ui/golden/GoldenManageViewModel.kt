package com.jugurdzija.homeshelf.ui.golden

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.GoldenItem
import com.jugurdzija.homeshelf.data.GoldenStore
import com.jugurdzija.homeshelf.data.StorageRepository
import com.jugurdzija.homeshelf.ui.nav.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class GoldenManageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val goldenStore: GoldenStore,
    private val storageRepository: StorageRepository
) : ViewModel() {

    sealed interface Event {
        data class NavigateToDetails(val name: String) : Event
    }

    sealed interface State {
        data object Loading : State
        data object Empty : State
        data class Loaded(val referenceName: String, val items: List<GoldenItem>) : State
    }

    private val storageId: String = checkNotNull(savedStateHandle[Routes.ARG_STORAGE_ID])

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val items = goldenStore.loadAll().filter { it.storageId == storageId }
            val referenceName = storageRepository.loadAll().firstOrNull { it.id == storageId }?.name ?: ""
            _state.value = if (items.isEmpty()) State.Empty
            else State.Loaded(referenceName, items)
        }
    }

    fun onItemClick(name: String) {
        viewModelScope.launch {
            _events.send(Event.NavigateToDetails(name))
        }
    }

    fun onDelete(name: String) {
        viewModelScope.launch {
            goldenStore.delete(name)
            load()
        }
    }

    fun formatTimestamp(iso: String): String {
        return try {
            val instant = Instant.parse(iso)
            val ldt = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
            ldt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
        } catch (e: Exception) {
            iso
        }
    }
}
