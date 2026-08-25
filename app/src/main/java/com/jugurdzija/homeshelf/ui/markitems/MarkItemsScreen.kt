package com.jugurdzija.homeshelf.ui.markitems

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jugurdzija.homeshelf.data.BoundingBox
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.MarkedItem
import com.jugurdzija.homeshelf.stt.VoiceInputState
import com.jugurdzija.homeshelf.ui.common.VoiceInputIcon
import com.jugurdzija.homeshelf.ui.theme.HomeShelfTheme
import com.jugurdzija.homeshelf.util.cellBoundsAsFraction
import com.jugurdzija.homeshelf.util.resolveCellName
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.core.graphics.createBitmap

private val GuideLineColor = Color(0xFFFFEB3B).copy(alpha = 0.35f)
private val BoxColor = Color(0xFF29B6F6)
private val SelectedBoxColor = Color(0xFFE53935)
private const val TapSlopPx = 12f
private const val HitPaddingDp = 20
private const val MinBoxSize = 0.03f
private const val DefaultBoxFractionOfShortSide = 0.08f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkItemsScreen(
    onBack: () -> Unit,
    vm: MarkItemsViewModel = hiltViewModel()
) {
    val bitmap by vm.bitmapState.collectAsState()
    val detectState by vm.detectState.collectAsState()
    val voiceInputState by vm.voiceInputState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(detectState) {
        val result = detectState
        if (result is DetectState.Error) {
            snackbarHostState.showSnackbar(result.message)
            vm.resetDetectState()
        }
    }

    LaunchedEffect(Unit) {
        vm.voiceError.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    MarkItemsScreenContent(
        storageName = vm.storageName,
        bitmap = bitmap,
        guideLines = vm.guideLines,
        markedItems = vm.markedItems,
        selectedId = vm.selectedId,
        isDetecting = detectState is DetectState.Loading,
        voiceInputState = voiceInputState,
        onBack = {
            vm.confirmSelection()
            onBack()
        },
        onCreateItem = vm::createItem,
        onSelect = vm::select,
        onUpdateBoundingBox = vm::updateBoundingBox,
        onUpdateName = vm::updateName,
        onUpdateTransparent = vm::updateTransparent,
        onConfirmSelection = vm::confirmSelection,
        onDeleteItem = vm::deleteItem,
        onRunAiDetection = vm::runAiDetection,
        onStartVoiceInput = vm::startVoiceInput,
        onStopVoiceInput = vm::stopVoiceInput,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkItemsScreenContent(
    storageName: String,
    bitmap: Bitmap?,
    guideLines: List<GuideLine>,
    markedItems: List<MarkedItem>,
    selectedId: String?,
    isDetecting: Boolean,
    voiceInputState: VoiceInputState,
    onBack: () -> Unit,
    onCreateItem: (BoundingBox) -> Unit,
    onSelect: (String) -> Unit,
    onUpdateBoundingBox: (String, BoundingBox) -> Unit,
    onUpdateName: (String, String) -> Unit,
    onUpdateTransparent: (String, Boolean) -> Unit,
    onConfirmSelection: () -> Unit,
    onDeleteItem: (String) -> Unit,
    onRunAiDetection: (canvasWidth: Int, canvasHeight: Int) -> Unit,
    onStartVoiceInput: () -> Unit,
    onStopVoiceInput: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val selectedItem = markedItems.firstOrNull { it.id == selectedId }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    if (selectedItem != null) {
                        OutlinedTextField(
                            value = selectedItem.name,
                            onValueChange = { onUpdateName(selectedItem.id, it) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                VoiceInputIcon(
                                    state = voiceInputState,
                                    onStart = onStartVoiceInput,
                                    onStop = onStopVoiceInput
                                )
                            }
                        )
                    } else {
                        Text(storageName)
                    }
                },
                actions = {
                    if (selectedItem != null) {
                        Checkbox(
                            checked = selectedItem.isTransparentContainer,
                            onCheckedChange = { onUpdateTransparent(selectedItem.id, it) }
                        )
                        Text("Transparent", style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = onConfirmSelection) {
                            Icon(Icons.Default.Check, contentDescription = "Save item")
                        }
                        IconButton(onClick = { onDeleteItem(selectedItem.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete item",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        TextButton(
                            onClick = { onRunAiDetection(canvasSize.width, canvasSize.height) },
                            enabled = bitmap != null && canvasSize != IntSize.Zero && !isDetecting
                        ) {
                            if (isDetecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.width(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Auto Detect")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        val currentBitmap = bitmap
        if (currentBitmap == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                        .pointerInput(currentBitmap, guideLines, markedItems, selectedId) {
                            val hitPaddingPx = HitPaddingDp.dp.toPx()
                            awaitEachGesture {
                                val firstDown = awaitFirstDown(requireUnconsumed = false)
                                val hitId = hitTest(markedItems, firstDown.position, canvasSize, currentBitmap, hitPaddingPx)
                                val isOnSelected = hitId != null && hitId == selectedId

                                var totalDrag = Offset.Zero
                                var secondPointerId: PointerId? = null
                                var resizeStartSeparation: Offset? = null
                                var gestureBoxStart: BoundingBox? = null

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val active = event.changes.filter { it.pressed }
                                    if (active.isEmpty()) break

                                    if (active.size == 1 && secondPointerId == null) {
                                        val change = active.firstOrNull { it.id == firstDown.id }
                                        if (change != null) {
                                            val delta = change.positionChange()
                                            totalDrag += delta
                                            if (isOnSelected && totalDrag.getDistance() > TapSlopPx) {
                                                change.consume()
                                                val fracDelta = canvasDeltaToBitmapFraction(delta, canvasSize, currentBitmap)
                                                val current = markedItems.firstOrNull { it.id == selectedId }?.boundingBox
                                                if (current != null) {
                                                    val moved = current.copy(
                                                        x = (current.x + fracDelta.x).coerceIn(0f, 1f - current.width),
                                                        y = (current.y + fracDelta.y).coerceIn(0f, 1f - current.height)
                                                    )
                                                    onUpdateBoundingBox(selectedId, moved)
                                                }
                                            }
                                        }
                                    } else if (active.size >= 2 && selectedId != null) {
                                        if (secondPointerId == null) {
                                            val newPointer = active.firstOrNull { it.id != firstDown.id }
                                            val p1 = active.firstOrNull { it.id == firstDown.id }
                                            if (newPointer != null && p1 != null) {
                                                secondPointerId = newPointer.id
                                                resizeStartSeparation = Offset(
                                                    abs(p1.position.x - newPointer.position.x),
                                                    abs(p1.position.y - newPointer.position.y)
                                                )
                                                gestureBoxStart = markedItems.firstOrNull { it.id == selectedId }?.boundingBox
                                            }
                                        } else {
                                            val p1 = active.firstOrNull { it.id == firstDown.id }
                                            val p2 = active.firstOrNull { it.id == secondPointerId }
                                            val startSep = resizeStartSeparation
                                            val boxStart = gestureBoxStart
                                            if (p1 != null && p2 != null && startSep != null && boxStart != null) {
                                                p1.consume()
                                                p2.consume()
                                                val currentSep = Offset(
                                                    abs(p1.position.x - p2.position.x),
                                                    abs(p1.position.y - p2.position.y)
                                                )
                                                val sepDeltaPx = currentSep - startSep
                                                val fracDelta = canvasDeltaToBitmapFraction(sepDeltaPx, canvasSize, currentBitmap)
                                                val newWidth = (boxStart.width + fracDelta.x)
                                                    .coerceIn(MinBoxSize, 1f - boxStart.x)
                                                val newHeight = (boxStart.height + fracDelta.y)
                                                    .coerceIn(MinBoxSize, 1f - boxStart.y)
                                                onUpdateBoundingBox(selectedId, boxStart.copy(width = newWidth, height = newHeight))
                                            }
                                        }
                                    }
                                }

                                val wasTap = secondPointerId == null && totalDrag.getDistance() <= TapSlopPx
                                if (wasTap) {
                                    if (selectedId != null && selectedId != hitId) {
                                        onConfirmSelection()
                                    }
                                    if (hitId != null) {
                                        if (hitId != selectedId) onSelect(hitId)
                                    } else {
                                        val box = computeDefaultBox(firstDown.position, canvasSize, currentBitmap, guideLines)
                                        onCreateItem(box)
                                    }
                                }
                            }
                        }
                ) {
                    val strokeWidth = 2.dp.toPx()
                    guideLines.forEach { line ->
                        if (line.isHorizontal) {
                            val y = line.position * size.height
                            drawLine(GuideLineColor, Offset(0f, y), Offset(size.width, y), strokeWidth)
                        } else {
                            val x = line.position * size.width
                            drawLine(GuideLineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth)
                        }
                    }
                    markedItems.forEach { item ->
                        val rect = bitmapFractionToCanvasRect(item.boundingBox, canvasSize, currentBitmap)
                        val color = if (item.id == selectedId) SelectedBoxColor else BoxColor
                        drawRect(color, topLeft = rect.topLeft, size = rect.size, style = Stroke(width = strokeWidth))
                    }
                }

                markedItems.forEach { item ->
                    if (canvasSize != IntSize.Zero) {
                        val rect = bitmapFractionToCanvasRect(item.boundingBox, canvasSize, currentBitmap)
                        val label = item.name.ifBlank { "…" }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier
                                .offset { IntOffset(rect.left.roundToInt(), (rect.top - 20).roundToInt()) }
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                if (markedItems.isEmpty()) {
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
                            text = "No items marked yet — tap the photo to add one",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun hitTest(
    items: List<MarkedItem>,
    position: Offset,
    canvasSize: IntSize,
    bitmap: Bitmap,
    paddingPx: Float
): String? {
    if (canvasSize == IntSize.Zero) return null
    return items.lastOrNull { item ->
        val rect = bitmapFractionToCanvasRect(item.boundingBox, canvasSize, bitmap)
        val expanded = Rect(
            rect.left - paddingPx, rect.top - paddingPx,
            rect.right + paddingPx, rect.bottom + paddingPx
        )
        expanded.contains(position)
    }?.id
}

private fun computeDefaultBox(
    tapPosition: Offset,
    canvasSize: IntSize,
    bitmap: Bitmap,
    guideLines: List<GuideLine>
): BoundingBox {
    val center = canvasToBitmapFraction(tapPosition, canvasSize, bitmap)
    val cellName = resolveCellName(
        guideLines, canvasSize.width, canvasSize.height, bitmap.width, bitmap.height, center.x, center.y
    )
    val cellBounds = cellName?.let {
        cellBoundsAsFraction(guideLines, canvasSize.width, canvasSize.height, bitmap.width, bitmap.height, it)
    }
    val shortSidePx = min(bitmap.width, bitmap.height) * DefaultBoxFractionOfShortSide
    val (w, h) = if (cellBounds != null) {
        (cellBounds.width() * 0.5f) to (cellBounds.height() * 0.5f)
    } else {
        (shortSidePx / bitmap.width) to (shortSidePx / bitmap.height)
    }
    val width = w.coerceIn(MinBoxSize, 1f)
    val height = h.coerceIn(MinBoxSize, 1f)
    val x = (center.x - width / 2f).coerceIn(0f, 1f - width)
    val y = (center.y - height / 2f).coerceIn(0f, 1f - height)
    return BoundingBox(x, y, width, height)
}

private fun canvasToBitmapFraction(offset: Offset, canvasSize: IntSize, bitmap: Bitmap): Offset {
    val scale = min(canvasSize.width.toFloat() / bitmap.width, canvasSize.height.toFloat() / bitmap.height)
    val renderedW = bitmap.width * scale
    val renderedH = bitmap.height * scale
    val offsetX = (canvasSize.width - renderedW) / 2f
    val offsetY = (canvasSize.height - renderedH) / 2f
    return Offset(
        ((offset.x - offsetX) / renderedW).coerceIn(0f, 1f),
        ((offset.y - offsetY) / renderedH).coerceIn(0f, 1f)
    )
}

private fun canvasDeltaToBitmapFraction(delta: Offset, canvasSize: IntSize, bitmap: Bitmap): Offset {
    val scale = min(canvasSize.width.toFloat() / bitmap.width, canvasSize.height.toFloat() / bitmap.height)
    val renderedW = bitmap.width * scale
    val renderedH = bitmap.height * scale
    return Offset(delta.x / renderedW, delta.y / renderedH)
}

private fun bitmapFractionToCanvasRect(box: BoundingBox, canvasSize: IntSize, bitmap: Bitmap): Rect {
    val scale = min(canvasSize.width.toFloat() / bitmap.width, canvasSize.height.toFloat() / bitmap.height)
    val renderedW = bitmap.width * scale
    val renderedH = bitmap.height * scale
    val offsetX = (canvasSize.width - renderedW) / 2f
    val offsetY = (canvasSize.height - renderedH) / 2f
    val left = offsetX + box.x * renderedW
    val top = offsetY + box.y * renderedH
    return Rect(left, top, left + box.width * renderedW, top + box.height * renderedH)
}

private fun createPreviewBitmap(): Bitmap {
    val bitmap = createBitmap(400, 600)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.rgb(210, 210, 210))
    return bitmap
}

private val previewItems = listOf(
    MarkedItem(id = "1", name = "Rice", boundingBox = BoundingBox(0.1f, 0.1f, 0.3f, 0.15f)),
    MarkedItem(
        id = "2", name = "Sugar", boundingBox = BoundingBox(0.5f, 0.4f, 0.25f, 0.2f),
        isTransparentContainer = true
    )
)

@Preview(showBackground = true)
@Composable
private fun MarkItemsScreenLoadedPreview() {
    HomeShelfTheme {
        MarkItemsScreenContent(
            storageName = "Pantry Shelf A",
            bitmap = createPreviewBitmap(),
            guideLines = emptyList(),
            markedItems = previewItems,
            selectedId = null,
            isDetecting = false,
            voiceInputState = VoiceInputState.IDLE,
            onBack = {},
            onCreateItem = {},
            onSelect = {},
            onUpdateBoundingBox = { _, _ -> },
            onUpdateName = { _, _ -> },
            onUpdateTransparent = { _, _ -> },
            onConfirmSelection = {},
            onDeleteItem = {},
            onRunAiDetection = { _, _ -> },
            onStartVoiceInput = {},
            onStopVoiceInput = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MarkItemsScreenNoItemsPreview() {
    HomeShelfTheme {
        MarkItemsScreenContent(
            storageName = "Pantry Shelf A",
            bitmap = createPreviewBitmap(),
            guideLines = emptyList(),
            markedItems = emptyList(),
            selectedId = null,
            isDetecting = false,
            voiceInputState = VoiceInputState.IDLE,
            onBack = {},
            onCreateItem = {},
            onSelect = {},
            onUpdateBoundingBox = { _, _ -> },
            onUpdateName = { _, _ -> },
            onUpdateTransparent = { _, _ -> },
            onConfirmSelection = {},
            onDeleteItem = {},
            onRunAiDetection = { _, _ -> },
            onStartVoiceInput = {},
            onStopVoiceInput = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MarkItemsScreenSelectedPreview() {
    HomeShelfTheme {
        MarkItemsScreenContent(
            storageName = "Pantry Shelf A",
            bitmap = createPreviewBitmap(),
            guideLines = emptyList(),
            markedItems = previewItems,
            selectedId = "2",
            isDetecting = false,
            voiceInputState = VoiceInputState.IDLE,
            onBack = {},
            onCreateItem = {},
            onSelect = {},
            onUpdateBoundingBox = { _, _ -> },
            onUpdateName = { _, _ -> },
            onUpdateTransparent = { _, _ -> },
            onConfirmSelection = {},
            onDeleteItem = {},
            onRunAiDetection = { _, _ -> },
            onStartVoiceInput = {},
            onStopVoiceInput = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
