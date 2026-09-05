package com.jugurdzija.homeshelf.ui.edit

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.ui.theme.HomeShelfTheme
import com.jugurdzija.homeshelf.usecase.StorageSaveResult
import kotlin.math.abs
import androidx.core.graphics.createBitmap

private val GuideLineYellow = Color(0xFFFFEB3B)
private val GuideLineRed = Color(0xFFE53935)
private const val DragThreshold = 40f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGuidelinesScreen(
    onSaved: () -> Unit,
    onDiscarded: () -> Unit,
    vm: EditGuidelinesViewModel = hiltViewModel()
) {
    val bitmap by vm.bitmapState.collectAsState()
    val saveState by vm.saveState.collectAsState()
    val gridGenerateState by vm.gridGenerateState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(vm.navEvent) {
        vm.navEvent.collect { event ->
            when (event) {
                is EditNavEvent.Saved -> onSaved()
                is EditNavEvent.Discarded -> onDiscarded()
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

    LaunchedEffect(gridGenerateState) {
        val result = gridGenerateState
        if (result is GridGenerateResult.Error) {
            snackbarHostState.showSnackbar(result.message)
            vm.resetGridGenerateState()
        }
    }

    EditScreenContent(
        bitmap = bitmap,
        guideLines = vm.guideLines,
        onAddGuideLine = { isHorizontal ->
            vm.guideLines.add(GuideLine(vm.nextId++, isHorizontal = isHorizontal, position = 0.5f))
        },
        onDeleteGuideLine = { id -> vm.guideLines.removeAll { it.id == id } },
        onUpdateGuideLinePosition = { id, position ->
            val idx = vm.guideLines.indexOfFirst { it.id == id }
            if (idx >= 0) {
                vm.guideLines[idx] = vm.guideLines[idx].copy(position = position)
            }
        },
        saveEnabled = bitmap != null,
        onSave = vm::save,
        onDiscard = vm::discard,
        onGenerateGridLines = vm::generateGridLines,
        isGenerating = gridGenerateState is GridGenerateResult.Loading,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditScreenContent(
    bitmap: Bitmap?,
    guideLines: List<GuideLine>,
    onAddGuideLine: (isHorizontal: Boolean) -> Unit,
    onDeleteGuideLine: (id: Int) -> Unit,
    onUpdateGuideLinePosition: (id: Int, position: Float) -> Unit,
    saveEnabled: Boolean,
    onSave: (canvasWidth: Int, canvasHeight: Int) -> Unit,
    onDiscard: () -> Unit,
    onGenerateGridLines: (canvasWidth: Int, canvasHeight: Int) -> Unit,
    isGenerating: Boolean,
    snackbarHostState: SnackbarHostState
) {
    var selectedId by remember { mutableStateOf(-1) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Guidelines") },
                navigationIcon = {
                    IconButton(onClick = onDiscard) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Discard")
                    }
                },
                actions = {
                    if (selectedId != -1) {
                        IconButton(onClick = {
                            onDeleteGuideLine(selectedId)
                            selectedId = -1
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete line",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    TextButton(
                        onClick = { onGenerateGridLines(canvasSize.width, canvasSize.height) },
                        enabled = bitmap != null && canvasSize != IntSize.Zero && !isGenerating
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(16.dp).width(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Auto Grid")
                        }
                    }
                    TextButton(onClick = { onAddGuideLine(true) }) { Text("+H") }
                    TextButton(onClick = { onAddGuideLine(false) }) { Text("+V") }
                    TextButton(
                        onClick = { onSave(canvasSize.width, canvasSize.height) },
                        enabled = saveEnabled
                    ) { Text("Save") }
                }
            )
        }
    ) { padding ->
        val currentBitmap = bitmap
        if (currentBitmap == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val hit = guideLines.firstOrNull { line ->
                                    val dist = if (line.isHorizontal)
                                        abs(offset.y - line.position * size.height)
                                    else
                                        abs(offset.x - line.position * size.width)
                                    dist < DragThreshold
                                }
                                selectedId = hit?.id ?: -1
                            }
                        }
                        .pointerInput(Unit) {
                            var dragTargetId = -1
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val target = guideLines.firstOrNull { line ->
                                        val dist = if (line.isHorizontal)
                                            abs(offset.y - line.position * size.height)
                                        else
                                            abs(offset.x - line.position * size.width)
                                        dist < DragThreshold
                                    }
                                    dragTargetId = target?.id ?: -1
                                    if (dragTargetId >= 0) {
                                        selectedId = dragTargetId
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val id = dragTargetId
                                    val line = guideLines.firstOrNull { it.id == id }
                                    if (line != null) {
                                        val newPos = if (line.isHorizontal)
                                            (change.position.y / size.height).coerceIn(0f, 1f)
                                        else
                                            (change.position.x / size.width).coerceIn(0f, 1f)
                                        onUpdateGuideLinePosition(id, newPos)
                                    }
                                },
                                onDragEnd = { dragTargetId = -1 },
                                onDragCancel = { dragTargetId = -1 }
                            )
                        }
                ) {
                    val strokeWidth = 2.dp.toPx()
                    guideLines.forEach { line ->
                        val color = if (line.id == selectedId) GuideLineRed else GuideLineYellow
                        if (line.isHorizontal) {
                            val y = line.position * size.height
                            drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth)
                        } else {
                            val x = line.position * size.width
                            drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth)
                        }
                    }
                }

                Text(
                    text = "${currentBitmap.width} × ${currentBitmap.height} px",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )

                if (guideLines.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.height(16.dp).width(16.dp)
                        )
                        Text(
                            text = "No grid set — the whole photo will be compared as one area",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun createPreviewBitmap(): Bitmap {
    val bitmap = createBitmap(400, 600)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.rgb(210, 210, 210))
    return bitmap
}

private val previewGuideLines = listOf(
    GuideLine(id = 0, isHorizontal = true, position = 0.33f),
    GuideLine(id = 1, isHorizontal = true, position = 0.66f),
    GuideLine(id = 2, isHorizontal = false, position = 0.5f)
)

@Preview(showBackground = true)
@Composable
private fun EditScreenLoadingPreview() {
    HomeShelfTheme {
        EditScreenContent(
            bitmap = null,
            guideLines = emptyList(),
            onAddGuideLine = {},
            onDeleteGuideLine = {},
            onUpdateGuideLinePosition = { _, _ -> },
            saveEnabled = false,
            onSave = { _, _ -> },
            onDiscard = {},
            onGenerateGridLines = { _, _ -> },
            isGenerating = false,
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditScreenWithGuideLinesPreview() {
    HomeShelfTheme {
        EditScreenContent(
            bitmap = createPreviewBitmap(),
            guideLines = previewGuideLines,
            onAddGuideLine = {},
            onDeleteGuideLine = {},
            onUpdateGuideLinePosition = { _, _ -> },
            saveEnabled = true,
            onSave = { _, _ -> },
            onDiscard = {},
            onGenerateGridLines = { _, _ -> },
            isGenerating = false,
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditScreenNoGridPreview() {
    HomeShelfTheme {
        EditScreenContent(
            bitmap = createPreviewBitmap(),
            guideLines = emptyList(),
            onAddGuideLine = {},
            onDeleteGuideLine = {},
            onUpdateGuideLinePosition = { _, _ -> },
            saveEnabled = true,
            onSave = { _, _ -> },
            onDiscard = {},
            onGenerateGridLines = { _, _ -> },
            isGenerating = false,
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
