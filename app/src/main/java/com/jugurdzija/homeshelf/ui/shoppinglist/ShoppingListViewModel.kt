package com.jugurdzija.homeshelf.ui.shoppinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.ShoppingListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ShoppingListUiState>(ShoppingListUiState.Loading)
    val state: StateFlow<ShoppingListUiState> = _state.asStateFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            val items = shoppingListRepository.loadAll()
            _state.value = if (items.isEmpty()) ShoppingListUiState.Empty else ShoppingListUiState.Loaded(items)
        }
    }

    fun onAdd(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            shoppingListRepository.add(trimmed)
            reload()
        }
    }

    fun onRemove(id: String) {
        viewModelScope.launch {
            shoppingListRepository.remove(id)
            reload()
        }
    }
}
