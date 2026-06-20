package com.jugurdzija.homeshelf.ui.edit

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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

private val GuideLineYellow = Color(0xFFFFEB3B)
private val GuideLineRed = Color(0xFFE53935)
private const val DragThreshold = 40f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    onSaved: () -> Unit,
    onDiscarded: () -> Unit,
    vm: EditViewModel = hiltViewModel()
) {
    val bitmap by vm.bitmapState.collectAsState()
    val saveState by vm.saveState.collectAsState()
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

    EditScreenContent(
        bitmap = bitmap,
        isNewStorage = vm.isNewStorage,
        name = vm.name,
        onNameChange = { vm.name = it },
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
        saveEnabled = bitmap != null && (!vm.isNewStorage || vm.name.isNotBlank()),
        onSave = vm::save,
        onDiscard = vm::discard,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditScreenContent(
    bitmap: Bitmap?,
    isNewStorage: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    guideLines: List<GuideLine>,
    onAddGuideLine: (isHorizontal: Boolean) -> Unit,
    onDeleteGuideLine: (id: Int) -> Unit,
    onUpdateGuideLinePosition: (id: Int, position: Float) -> Unit,
    saveEnabled: Boolean,
    onSave: (canvasWidth: Int, canvasHeight: Int) -> Unit,
    onDiscard: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var selectedId by remember { mutableStateOf(-1) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var showNameSheet by remember { mutableStateOf(isNewStorage) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (isNewStorage && name.isBlank()) "New Storage" else name) },
                navigationIcon = {
                    IconButton(onClick = onDiscard) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Discard")
                    }
                },
                actions = {
                    if (isNewStorage) {
                        IconButton(onClick = { showNameSheet = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Name storage")
                        }
                    }
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
                    TextButton(onClick = { onAddGuideLine(true) }) { Text("+ H") }
                    TextButton(onClick = { onAddGuideLine(false) }) { Text("+ V") }
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
            }
        }
    }

    if (showNameSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNameSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Name this storage",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Storage name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showNameSheet = false },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Done") }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun createPreviewBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(400, 600, Bitmap.Config.ARGB_8888)
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
            isNewStorage = false,
            name = "Pantry Shelf A",
            onNameChange = {},
            guideLines = emptyList(),
            onAddGuideLine = {},
            onDeleteGuideLine = {},
            onUpdateGuideLinePosition = { _, _ -> },
            saveEnabled = false,
            onSave = { _, _ -> },
            onDiscard = {},
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
            isNewStorage = false,
            name = "Pantry Shelf A",
            onNameChange = {},
            guideLines = previewGuideLines,
            onAddGuideLine = {},
            onDeleteGuideLine = {},
            onUpdateGuideLinePosition = { _, _ -> },
            saveEnabled = true,
            onSave = { _, _ -> },
            onDiscard = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditScreenNewStoragePreview() {
    HomeShelfTheme {
        EditScreenContent(
            bitmap = createPreviewBitmap(),
            isNewStorage = true,
            name = "",
            onNameChange = {},
            guideLines = previewGuideLines,
            onAddGuideLine = {},
            onDeleteGuideLine = {},
            onUpdateGuideLinePosition = { _, _ -> },
            saveEnabled = false,
            onSave = { _, _ -> },
            onDiscard = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
