package com.jugurdzija.homeshelf.ui.shoppinglist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.domain.usecases.getstorage.GetStorageUseCase
import com.jugurdzija.homeshelf.domain.usecases.shoppinglist.ShoppingListUseCase
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
    private val shoppingListUseCase: ShoppingListUseCase,
    private val getStorageUseCase: GetStorageUseCase
) : ViewModel() {

    val itemId: String = checkNotNull(savedStateHandle[Routes.ARG_ITEM_ID])

    private val _state = MutableStateFlow<ShoppingListItemDetailUiState>(ShoppingListItemDetailUiState.Loading)
    val state: StateFlow<ShoppingListItemDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val item = shoppingListUseCase.getItem(itemId)
            val storageName = item?.storageId?.let { storageId -> getStorageUseCase.getStorage(storageId)?.name }
            _state.value = ShoppingListItemDetailUiState.Loaded(item?.name ?: "", storageName)
        }
    }
}
