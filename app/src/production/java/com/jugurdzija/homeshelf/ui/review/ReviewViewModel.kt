package com.jugurdzija.homeshelf.ui.review

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jugurdzija.homeshelf.data.MarkedItem
import com.jugurdzija.homeshelf.data.PendingCaptureStore
import com.jugurdzija.homeshelf.data.ShoppingListRepository
import com.jugurdzija.homeshelf.data.StorageRepository
import com.jugurdzija.homeshelf.llm.CellPair
import com.jugurdzija.homeshelf.llm.ItemChange
import com.jugurdzija.homeshelf.llm.KnownItem
import com.jugurdzija.homeshelf.llm.ShelfDiffAnalyzer
import com.jugurdzija.homeshelf.ui.nav.Routes
import com.jugurdzija.homeshelf.usecase.ComparisonPipeline
import com.jugurdzija.homeshelf.usecase.ComparisonResult
import com.jugurdzija.homeshelf.util.cellBoundsAsFraction
import com.jugurdzija.homeshelf.util.toCellLocalFraction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ReviewNavEvent {
    data object ToReference : ReviewNavEvent
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pendingCaptureStore: PendingCaptureStore,
    private val storageRepository: StorageRepository,
    private val comparisonPipeline: ComparisonPipeline,
    private val shelfDiffAnalyzer: ShelfDiffAnalyzer,
    private val shoppingListRepository: ShoppingListRepository
) : ViewModel() {

    val storageId: String = checkNotNull(savedStateHandle[Routes.ARG_STORAGE_ID])

    private val _state = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    private val _aiDiffState = MutableStateFlow<AiDiffState>(AiDiffState.NotRequested)
    val aiDiffState: StateFlow<AiDiffState> = _aiDiffState.asStateFlow()

    private val _navEvent = MutableSharedFlow<ReviewNavEvent>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<ReviewNavEvent> = _navEvent

    private val _shoppingListAdded = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val shoppingListAdded: SharedFlow<Int> = _shoppingListAdded

    init {
        viewModelScope.launch {
            val storageName = storageRepository.loadAll().firstOrNull { it.id == storageId }?.name ?: ""
            val pending = pendingCaptureStore.load()
            if (pending == null) {
                _state.value = ReviewUiState.CompareError(storageName, "No captured image found")
                return@launch
            }
            _state.value = when (val result = comparisonPipeline.run(pending, storageId)) {
                is ComparisonResult.Success -> ReviewUiState.Done(
                    storageName = storageName,
                    alignedBitmap = result.alignedBitmap,
                    guideLines = result.guideLines,
                    similarities = result.similarities,
                    referenceCells = result.referenceCells,
                    newCells = result.newCells,
                    markedItems = result.markedItems
                )
                ComparisonResult.AlignmentFailed -> ReviewUiState.CompareError(storageName, "Alignment failed — try capturing again")
                ComparisonResult.NoGuideLines -> ReviewUiState.CompareError(storageName, "No guide lines saved for this storage")
                ComparisonResult.NoEmbeddings -> ReviewUiState.CompareError(storageName, "No reference data saved for this storage")
                ComparisonResult.NoCells -> ReviewUiState.CompareError(storageName, "Could not extract grid cells")
            }
        }
    }

    fun addConsumedToShoppingList() {
        val diffState = _aiDiffState.value as? AiDiffState.Done ?: return
        val candidates = diffState.results.flatMap { cellResult ->
            cellResult.items.mapNotNull { item ->
                if (item.change == ItemChange.REMOVED || item.change == ItemChange.FULLY_CONSUMED) {
                    diffState.knownItemsById["${cellResult.cellId}:${item.id}"]?.let { it.name to storageId }
                } else {
                    null
                }
            }
        }
        viewModelScope.launch {
            val added = shoppingListRepository.addAutoDetected(candidates)
            _shoppingListAdded.emit(added.size)
        }
    }

    fun analyzeWithAi() {
        val done = _state.value as? ReviewUiState.Done ?: return
        if (_aiDiffState.value is AiDiffState.Loading) return
        _aiDiffState.value = AiDiffState.Loading
        viewModelScope.launch {
            val newCellsByName = done.newCells.associateBy { it.name }
            val itemsByCell = done.markedItems.filter { it.cellName != null }.groupBy { it.cellName!! }
            val bitmapWidth = done.alignedBitmap.width
            val bitmapHeight = done.alignedBitmap.height
            val knownItemsById = mutableMapOf<String, MarkedItem>()
            val pairs = done.referenceCells.mapNotNull { refCell ->
                val newCell = newCellsByName[refCell.name] ?: return@mapNotNull null
                val cellItems = itemsByCell[refCell.name].orEmpty()
                val cellBounds = if (cellItems.isNotEmpty()) {
                    cellBoundsAsFraction(done.guideLines, bitmapWidth, bitmapHeight, bitmapWidth, bitmapHeight, refCell.name)
                } else {
                    null
                }
                CellPair(
                    cellId = refCell.name,
                    referenceBitmap = refCell.bitmap,
                    newBitmap = newCell.bitmap,
                    knownItems = cellItems.mapIndexed { index, item ->
                        val shortId = (index + 1).toString()
                        knownItemsById["${refCell.name}:$shortId"] = item
                        KnownItem(
                            id = shortId,
                            name = item.name,
                            isTransparentContainer = item.isTransparentContainer,
                            box = if (cellBounds != null) toCellLocalFraction(item.boundingBox, cellBounds) else null
                        )
                    }
                )
            }
            val result = shelfDiffAnalyzer.analyze(pairs)
            _aiDiffState.value = result.fold(
                onSuccess = { AiDiffState.Done(it, knownItemsById) },
                onFailure = { AiDiffState.Error(it.message ?: "AI analysis failed") }
            )
        }
    }

    fun discard() {
        viewModelScope.launch {
            pendingCaptureStore.clear()
            _navEvent.emit(ReviewNavEvent.ToReference)
        }
    }
}
