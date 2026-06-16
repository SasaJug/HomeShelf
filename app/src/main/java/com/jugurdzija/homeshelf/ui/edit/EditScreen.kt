package com.jugurdzija.homeshelf.ui.edit

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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.usecase.StorageSaveResult
import kotlin.math.abs

private val GuideLineYellow = Color(0xFFFFEB3B)
private val GuideLineRed = Color(0xFFE53935)
private const val DragThreshold = 40f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    storageId: String?,
    onSaved: () -> Unit,
    onDiscarded: () -> Unit,
    vm: EditViewModel = hiltViewModel()
) {
    val bitmap by vm.bitmapState.collectAsState()
    val saveState by vm.saveState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedId by remember { mutableStateOf(-1) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var showNameSheet by remember { mutableStateOf(vm.isNewStorage) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isNewStorage && vm.name.isBlank()) "New Storage" else vm.name) },
                navigationIcon = {
                    IconButton(onClick = vm::discard) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Discard")
                    }
                },
                actions = {
                    if (vm.isNewStorage) {
                        IconButton(onClick = { showNameSheet = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Name storage")
                        }
                    }
                    if (selectedId != -1) {
                        IconButton(onClick = {
                            vm.guideLines.removeAll { it.id == selectedId }
                            selectedId = -1
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete line",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    TextButton(onClick = {
                        vm.guideLines.add(GuideLine(vm.nextId++, isHorizontal = true, position = 0.5f))
                    }) { Text("+ H") }
                    TextButton(onClick = {
                        vm.guideLines.add(GuideLine(vm.nextId++, isHorizontal = false, position = 0.5f))
                    }) { Text("+ V") }
                    TextButton(
                        onClick = { vm.save(canvasSize.width, canvasSize.height) },
                        enabled = bitmap != null && (!vm.isNewStorage || vm.name.isNotBlank())
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
                                val hit = vm.guideLines.firstOrNull { line ->
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
                            var dragTargetIndex = -1
                            detectDragGestures(
                                onDragStart = { offset ->
                                    dragTargetIndex = vm.guideLines.indexOfFirst { line ->
                                        val dist = if (line.isHorizontal)
                                            abs(offset.y - line.position * size.height)
                                        else
                                            abs(offset.x - line.position * size.width)
                                        dist < DragThreshold
                                    }
                                    if (dragTargetIndex >= 0) {
                                        selectedId = vm.guideLines[dragTargetIndex].id
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val idx = dragTargetIndex
                                    if (idx >= 0 && idx < vm.guideLines.size) {
                                        val line = vm.guideLines[idx]
                                        val newPos = if (line.isHorizontal)
                                            (change.position.y / size.height).coerceIn(0f, 1f)
                                        else
                                            (change.position.x / size.width).coerceIn(0f, 1f)
                                        vm.guideLines[idx] = line.copy(position = newPos)
                                    }
                                },
                                onDragEnd = { dragTargetIndex = -1 },
                                onDragCancel = { dragTargetIndex = -1 }
                            )
                        }
                ) {
                    val strokeWidth = 2.dp.toPx()
                    vm.guideLines.forEach { line ->
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
                    value = vm.name,
                    onValueChange = { vm.name = it },
                    label = { Text("Storage name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showNameSheet = false },
                    enabled = vm.name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Done") }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
