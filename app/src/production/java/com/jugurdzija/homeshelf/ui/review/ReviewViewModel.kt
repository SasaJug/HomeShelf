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
import com.jugurdzija.homeshelf.usecase.ComparisonPipeline
import com.jugurdzija.homeshelf.usecase.ComparisonResult
import com.jugurdzija.homeshelf.usecase.StorageSavePipeline
import com.jugurdzija.homeshelf.usecase.StorageSaveResult
import com.jugurdzija.homeshelf.util.cellBoundsAsFraction
import com.jugurdzija.homeshelf.util.fromCellLocalFraction
import com.jugurdzija.homeshelf.util.toCellLocalFraction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface ReviewNavEvent {
    data object ToReference : ReviewNavEvent
    data class ToEdit(val storageId: String) : ReviewNavEvent
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pendingCaptureStore: PendingCaptureStore,
    private val storageRepository: StorageRepository,
    private val comparisonPipeline: ComparisonPipeline,
    private val storageSavePipeline: StorageSavePipeline,
    private val shelfDiffAnalyzer: ShelfDiffAnalyzer,
    private val shoppingListRepository: ShoppingListRepository
) : ViewModel() {

    val storageId: String = checkNotNull(savedStateHandle["storageId"])

    private val _state = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val state: StateFlow<ReviewUiState> = _state.asStateFlow()

    private val _saveState = MutableStateFlow<StorageSaveResult?>(null)
    val saveState: StateFlow<StorageSaveResult?> = _saveState.asStateFlow()

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

    fun save() {
        val done = _state.value as? ReviewUiState.Done ?: return
        val diffState = _aiDiffState.value as? AiDiffState.Done
        viewModelScope.launch {
            val bitmapWidth = done.alignedBitmap.width
            val bitmapHeight = done.alignedBitmap.height
            val resolvedMarkedItems = diffState?.let {
                resolveMarkedItems(done, it, bitmapWidth, bitmapHeight)
            }
            val result = storageSavePipeline.run(
                storageId,
                done.storageName,
                done.alignedBitmap,
                done.guideLines,
                bitmapWidth,
                bitmapHeight,
                resolvedMarkedItems
            )
            when (result) {
                is StorageSaveResult.Done -> {
                    pendingCaptureStore.clear()
                    _navEvent.emit(ReviewNavEvent.ToReference)
                }
                is StorageSaveResult.Error -> _saveState.value = result
            }
        }
    }

    /**
     * Resolves marked items against the AI diff:
     *
     * - **REMOVED:** Dropped (physically gone).
     * - **REPLACED:** Dropped and re-added with a fresh ID using the model's new name/box.
     * - **ADDED:** Created with a fresh ID (same as REPLACED).
     * - **Everything else:** Carried forward as-is (e.g., `FULLY_CONSUMED` empty containers and `UNKNOWN`).
     */
    private fun resolveMarkedItems(
        done: ReviewUiState.Done,
        diffState: AiDiffState.Done,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): List<MarkedItem> {
        val removedIds = mutableSetOf<String>()
        val created = mutableListOf<MarkedItem>()
        diffState.results.forEach { cellResult ->
            val cellBounds = cellBoundsAsFraction(
                done.guideLines, bitmapWidth, bitmapHeight, bitmapWidth, bitmapHeight, cellResult.cellId
            )
            cellResult.items.forEach { itemResult ->
                when (itemResult.change) {
                    ItemChange.ADDED, ItemChange.REPLACED -> {
                        val name = itemResult.name
                        val box = itemResult.box
                        val isTransparentContainer = itemResult.isTransparentContainer
                        if (name != null && box != null && isTransparentContainer != null) {
                            val fullBox = if (cellBounds != null) fromCellLocalFraction(box, cellBounds) else box
                            created.add(
                                MarkedItem(
                                    id = UUID.randomUUID().toString(),
                                    name = name,
                                    boundingBox = fullBox,
                                    cellName = cellResult.cellId,
                                    isTransparentContainer = isTransparentContainer
                                )
                            )
                            if (itemResult.change == ItemChange.REPLACED) {
                                diffState.knownItemsById["${cellResult.cellId}:${itemResult.id}"]?.let {
                                    removedIds.add(it.id)
                                }
                            }
                        }
                    }
                    ItemChange.REMOVED -> {
                        diffState.knownItemsById["${cellResult.cellId}:${itemResult.id}"]?.let {
                            removedIds.add(it.id)
                        }
                    }
                    else -> Unit
                }
            }
        }
        return done.markedItems.filterNot { it.id in removedIds } + created
    }

    fun resetSaveState() {
        _saveState.value = null
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

    fun navigateToEdit() {
        val done = _state.value as? ReviewUiState.Done ?: return
        viewModelScope.launch {
            pendingCaptureStore.save(done.alignedBitmap)
            _navEvent.emit(ReviewNavEvent.ToEdit(storageId))
        }
    }
}
