package com.jugurdzija.homeshelf.ui.shoppinglist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.ShoppingListRepository
import com.jugurdzija.homeshelf.data.StorageRepository
import com.jugurdzija.homeshelf.ui.nav.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShoppingListItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val shoppingListRepository: ShoppingListRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {

    val itemId: String = checkNotNull(savedStateHandle[Routes.ARG_ITEM_ID])

    private val _state = MutableStateFlow<ShoppingListItemDetailUiState>(ShoppingListItemDetailUiState.Loading)
    val state: StateFlow<ShoppingListItemDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val item = shoppingListRepository.loadAll().firstOrNull { it.id == itemId }
            val storageName = item?.storageId?.let { storageId ->
                storageRepository.loadAll().firstOrNull { it.id == storageId }?.name
            }
            _state.value = ShoppingListItemDetailUiState.Loaded(item?.name ?: "", storageName)
        }
    }
}
