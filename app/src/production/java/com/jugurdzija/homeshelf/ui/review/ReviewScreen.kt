package com.jugurdzija.homeshelf.ui.review

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jugurdzija.homeshelf.data.BoundingBox
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.MarkedItem
import com.jugurdzija.homeshelf.llm.CellDiffResult
import com.jugurdzija.homeshelf.llm.ItemChange
import com.jugurdzija.homeshelf.llm.ItemDiffResult
import com.jugurdzija.homeshelf.ui.theme.HomeShelfTheme
import com.jugurdzija.homeshelf.usecase.StorageSaveResult
import com.jugurdzija.homeshelf.util.cellBoundsAsFraction
import com.jugurdzija.homeshelf.util.fromCellLocalFraction
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.core.graphics.createBitmap

private val SimilarityGreen = Color(0xFF4CAF50)
private val SimilarityYellow = Color(0xFFFFEB3B)
private val SimilarityRed = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    onToReference: () -> Unit,
    onToEdit: (String) -> Unit,
    vm: ReviewViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val saveState by vm.saveState.collectAsState()
    val aiDiffState by vm.aiDiffState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(vm.navEvent) {
        vm.navEvent.collect { event ->
            when (event) {
                ReviewNavEvent.ToReference -> onToReference()
                is ReviewNavEvent.ToEdit -> onToEdit(event.storageId)
            }
        }
    }

    LaunchedEffect(saveState) {
        val result = saveState
        if (result is StorageSaveResult.Error) {
            snackbarHostState.showSnackbar(result.message)
            vm.resetSaveState()
        }
    }

    LaunchedEffect(vm.shoppingListAdded) {
        vm.shoppingListAdded.collect { addedCount ->
            snackbarHostState.showSnackbar(
                if (addedCount > 0) "Added $addedCount item(s) to shopping list" else "No new items to add"
            )
        }
    }

    ReviewScreenContent(
        state = state,
        aiDiffState = aiDiffState,
        snackbarHostState = snackbarHostState,
        onDiscard = vm::discard,
        onAnalyzeWithAi = vm::analyzeWithAi,
        onEdit = vm::navigateToEdit,
        onSave = vm::save,
        onAddToShoppingList = vm::addConsumedToShoppingList
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewScreenContent(
    state: ReviewUiState,
    aiDiffState: AiDiffState,
    snackbarHostState: SnackbarHostState,
    onDiscard: () -> Unit,
    onAnalyzeWithAi: () -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onAddToShoppingList: () -> Unit
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val topBarTitle = when (val s = state) {
        is ReviewUiState.Done -> s.storageName
        is ReviewUiState.CompareError -> s.storageName
        ReviewUiState.Loading -> "Analyzing…"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle) },
                navigationIcon = {
                    IconButton(onClick = onDiscard) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Discard")
                    }
                },
                actions = {
                    if (state is ReviewUiState.Done) {
                        TextButton(
                            onClick = onAnalyzeWithAi,
                            enabled = aiDiffState !is AiDiffState.Loading
                        ) { Text("Analyze with AI") }
                        if (aiDiffState is AiDiffState.Done) {
                            TextButton(onClick = onAddToShoppingList) { Text("Add to List") }
                        }
                        TextButton(onClick = onEdit) { Text("Edit") }
                        TextButton(onClick = onSave) { Text("Save") }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val s = state) {
                ReviewUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ReviewUiState.Done -> {
                    Image(
                        bitmap = s.alignedBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { canvasSize = it },
                        contentScale = ContentScale.Fit
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 2.dp.toPx()
                        s.guideLines.forEach { line ->
                            if (line.isHorizontal) {
                                val y = line.position * size.height
                                drawLine(Color.Yellow, Offset(0f, y), Offset(size.width, y), stroke)
                            } else {
                                val x = line.position * size.width
                                drawLine(Color.Yellow, Offset(x, 0f), Offset(x, size.height), stroke)
                            }
                        }
                    }
                    val hLines = remember(s.guideLines) {
                        s.guideLines.filter { it.isHorizontal }.sortedBy { it.position }
                    }
                    val vLines = remember(s.guideLines) {
                        s.guideLines.filter { !it.isHorizontal }.sortedBy { it.position }
                    }
                    SimilarityOverlay(
                        similarities = s.similarities,
                        hLines = hLines,
                        vLines = vLines,
                        canvasSize = canvasSize,
                        modifier = Modifier.fillMaxSize()
                    )
                    val changeBoxes = remember(aiDiffState, s.guideLines, s.alignedBitmap) {
                        (aiDiffState as? AiDiffState.Done)?.let {
                            buildChangeBoxes(
                                results = it.results,
                                knownItemsById = it.knownItemsById,
                                guideLines = s.guideLines,
                                bitmapWidth = s.alignedBitmap.width,
                                bitmapHeight = s.alignedBitmap.height
                            )
                        }.orEmpty()
                    }
                    ItemChangeOverlay(
                        changeBoxes = changeBoxes,
                        canvasSize = canvasSize,
                        modifier = Modifier.fillMaxSize()
                    )
                    AiDiffPanel(
                        aiDiffState = aiDiffState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                }
                is ReviewUiState.CompareError -> {
                    Text(
                        text = s.message,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SimilarityOverlay(
    similarities: Map<String, Float>,
    hLines: List<GuideLine>,
    vLines: List<GuideLine>,
    canvasSize: IntSize,
    modifier: Modifier = Modifier
) {
    if (canvasSize == IntSize.Zero) return
    Box(modifier = modifier) {
        similarities.forEach { (name, similarity) ->
            val colIdx = name[0] - 'A'
            val rowIdx = name.substring(1).toInt() - 1
            if (colIdx + 1 >= vLines.size || rowIdx + 1 >= hLines.size) return@forEach
            val cx = (vLines[colIdx].position + vLines[colIdx + 1].position) / 2f
            val cy = (hLines[rowIdx].position + hLines[rowIdx + 1].position) / 2f
            val xPx = (cx * canvasSize.width).toInt()
            val yPx = (cy * canvasSize.height).toInt()
            val labelColor = when {
                similarity >= 0.9f -> SimilarityGreen
                similarity >= 0.7f -> SimilarityYellow
                else -> SimilarityRed
            }
            Box(
                modifier = Modifier
                    .offset { IntOffset(xPx, yPx) }
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${"%.0f".format(similarity * 100)}%",
                    color = labelColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private data class ChangeBox(val label: String, val color: Color, val box: BoundingBox)

private fun buildChangeBoxes(
    results: List<CellDiffResult>,
    knownItemsById: Map<String, MarkedItem>,
    guideLines: List<GuideLine>,
    bitmapWidth: Int,
    bitmapHeight: Int
): List<ChangeBox> {
    val boxes = mutableListOf<ChangeBox>()
    results.forEach { cellResult ->
        val cellBounds = cellBoundsAsFraction(
            guideLines, bitmapWidth, bitmapHeight, bitmapWidth, bitmapHeight, cellResult.cellId
        )
        cellResult.items.forEach { item ->
            if (item.change == ItemChange.UNCHANGED) return@forEach
            val knownBox = knownItemsById["${cellResult.cellId}:${item.id}"]
            fun aiBoxAsFullFraction(): BoundingBox? =
                item.box?.let { box -> cellBounds?.let { fromCellLocalFraction(box, it) } }

            when (item.change) {
                ItemChange.ADDED -> aiBoxAsFullFraction()?.let {
                    boxes += ChangeBox(item.name ?: item.id, SimilarityGreen, it)
                }
                ItemChange.REPLACED -> {
                    knownBox?.let { boxes += ChangeBox(it.name, SimilarityRed, it.boundingBox) }
                    aiBoxAsFullFraction()?.let {
                        boxes += ChangeBox(item.name ?: item.id, SimilarityGreen, it)
                    }
                }
                ItemChange.REMOVED -> knownBox?.let {
                    boxes += ChangeBox(it.name, SimilarityRed, it.boundingBox)
                }
                ItemChange.PARTIALLY_CONSUMED -> knownBox?.let {
                    boxes += ChangeBox(it.name, SimilarityYellow, it.boundingBox)
                }
                ItemChange.FULLY_CONSUMED -> knownBox?.let {
                    boxes += ChangeBox(it.name, SimilarityRed, it.boundingBox)
                }
                ItemChange.PARTIALLY_FILLED, ItemChange.FULLY_FILLED -> knownBox?.let {
                    boxes += ChangeBox(it.name, SimilarityGreen, it.boundingBox)
                }
                ItemChange.UNKNOWN -> knownBox?.let {
                    boxes += ChangeBox(it.name, Color.Gray, it.boundingBox)
                }
                else -> Unit
            }
        }
    }
    return boxes
}

@Composable
private fun ItemChangeOverlay(
    changeBoxes: List<ChangeBox>,
    canvasSize: IntSize,
    modifier: Modifier = Modifier
) {
    if (canvasSize == IntSize.Zero || changeBoxes.isEmpty()) return
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 2.dp.toPx()
            changeBoxes.forEach { changeBox ->
                val box = changeBox.box
                val left = box.x * size.width
                val top = box.y * size.height
                val width = box.width * size.width
                val height = box.height * size.height
                drawRect(
                    color = changeBox.color,
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(width, height),
                    style = Stroke(width = stroke)
                )
            }
        }
        changeBoxes.forEach { changeBox ->
            val box = changeBox.box
            val xPx = (box.x * canvasSize.width).toInt()
            val yPx = ((box.y * canvasSize.height) - 20f).toInt().coerceAtLeast(0)
            Box(
                modifier = Modifier
                    .offset { IntOffset(xPx, yPx) }
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = changeBox.label,
                    color = changeBox.color,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AiDiffPanel(
    aiDiffState: AiDiffState,
    modifier: Modifier = Modifier
) {
    when (aiDiffState) {
        AiDiffState.NotRequested, is AiDiffState.Done -> Unit
        AiDiffState.Loading -> {
            Box(
                modifier = modifier
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
        is AiDiffState.Error -> {
            Text(
                text = "AI analysis failed: ${aiDiffState.message}",
                modifier = modifier
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun previewBitmap(): Bitmap {
    val bitmap = createBitmap(360, 480)
    android.graphics.Canvas(bitmap).drawColor(android.graphics.Color.DKGRAY)
    return bitmap
}

private val previewGuideLines = listOf(
    GuideLine(id = 0, isHorizontal = true, position = 0f),
    GuideLine(id = 1, isHorizontal = true, position = 0.5f),
    GuideLine(id = 2, isHorizontal = true, position = 1f),
    GuideLine(id = 3, isHorizontal = false, position = 0f),
    GuideLine(id = 4, isHorizontal = false, position = 0.5f),
    GuideLine(id = 5, isHorizontal = false, position = 1f)
)

private val previewSimilarities = mapOf(
    "A1" to 0.95f,
    "A2" to 0.78f,
    "B1" to 0.42f,
    "B2" to 0.88f
)

private val previewDoneState = ReviewUiState.Done(
    storageName = "Pantry Shelf A",
    alignedBitmap = previewBitmap(),
    guideLines = previewGuideLines,
    similarities = previewSimilarities,
    referenceCells = emptyList(),
    newCells = emptyList(),
    markedItems = emptyList()
)

private val previewAiDiffResults = listOf(
    CellDiffResult(
        cellId = "A1",
        items = listOf(
            ItemDiffResult(id = "1", change = ItemChange.UNCHANGED, description = "Cereal box unchanged", name = "Cereal")
        )
    ),
    CellDiffResult(
        cellId = "B1",
        items = listOf(
            ItemDiffResult(id = "1", change = ItemChange.REMOVED, description = "Soup cans gone", name = "Soup"),
            ItemDiffResult(
                id = "N1",
                change = ItemChange.ADDED,
                description = "New pasta box detected",
                name = "Pasta",
                box = BoundingBox(0.2f, 0.3f, 0.4f, 0.3f),
                isTransparentContainer = false
            )
        )
    )
)

private val previewKnownItemsById = mapOf(
    "B1:1" to MarkedItem(
        id = "soup-1",
        name = "Soup",
        boundingBox = BoundingBox(0.55f, 0.15f, 0.3f, 0.25f),
        cellName = "B1"
    )
)

@Preview(showBackground = true)
@Composable
private fun ReviewScreenLoadingPreview() {
    HomeShelfTheme {
        ReviewScreenContent(
            state = ReviewUiState.Loading,
            aiDiffState = AiDiffState.NotRequested,
            snackbarHostState = remember { SnackbarHostState() },
            onDiscard = {},
            onAnalyzeWithAi = {},
            onEdit = {},
            onSave = {},
            onAddToShoppingList = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewScreenDonePreview() {
    HomeShelfTheme {
        ReviewScreenContent(
            state = previewDoneState,
            aiDiffState = AiDiffState.NotRequested,
            snackbarHostState = remember { SnackbarHostState() },
            onDiscard = {},
            onAnalyzeWithAi = {},
            onEdit = {},
            onSave = {},
            onAddToShoppingList = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewScreenAiLoadingPreview() {
    HomeShelfTheme {
        ReviewScreenContent(
            state = previewDoneState,
            aiDiffState = AiDiffState.Loading,
            snackbarHostState = remember { SnackbarHostState() },
            onDiscard = {},
            onAnalyzeWithAi = {},
            onEdit = {},
            onSave = {},
            onAddToShoppingList = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewScreenAiDonePreview() {
    HomeShelfTheme {
        ReviewScreenContent(
            state = previewDoneState,
            aiDiffState = AiDiffState.Done(previewAiDiffResults, previewKnownItemsById),
            snackbarHostState = remember { SnackbarHostState() },
            onDiscard = {},
            onAnalyzeWithAi = {},
            onEdit = {},
            onSave = {},
            onAddToShoppingList = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReviewScreenCompareErrorPreview() {
    HomeShelfTheme {
        ReviewScreenContent(
            state = ReviewUiState.CompareError(
                storageName = "Pantry Shelf A",
                message = "Alignment failed — try capturing again"
            ),
            aiDiffState = AiDiffState.NotRequested,
            snackbarHostState = remember { SnackbarHostState() },
            onDiscard = {},
            onAnalyzeWithAi = {},
            onEdit = {},
            onSave = {},
            onAddToShoppingList = {}
        )
    }
}
