package com.jugurdzija.homeshelf.ui.golden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.GoldenItem
import com.jugurdzija.homeshelf.data.GoldenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class GoldenManageViewModel @Inject constructor(
    private val goldenStore: GoldenStore
) : ViewModel() {

    sealed interface State {
        data object Loading : State
        data object Empty : State
        data class Loaded(val groups: Map<String, List<GoldenItem>>) : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _navigateToView = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToView: SharedFlow<Unit> = _navigateToView.asSharedFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val items = goldenStore.loadAll()
            _state.value = if (items.isEmpty()) State.Empty
            else State.Loaded(items.groupBy { it.referenceLabel })
        }
    }

    fun onItemClick(name: String) {
        viewModelScope.launch {
            goldenStore.loadIntoHolder(name)
            _navigateToView.emit(Unit)
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
