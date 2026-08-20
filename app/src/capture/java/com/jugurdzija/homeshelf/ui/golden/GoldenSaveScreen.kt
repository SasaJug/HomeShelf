package com.jugurdzija.homeshelf.ui.golden

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jugurdzija.homeshelf.data.BoundingBox
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.llm.ItemChange
import com.jugurdzija.homeshelf.util.cellBoundsAsFraction
import com.jugurdzija.homeshelf.util.resolveCellName
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

private val KnownBoxColor = Color(0xFF29B6F6)
private val NewBoxColor = Color(0xFF66BB6A)
private val SelectedBoxColor = Color(0xFFE53935)
private const val TapSlopPx = 12f
private const val HitPaddingDp = 20
private const val MinBoxSize = 0.03f
private const val DefaultBoxFractionOfShortSide = 0.08f

private data class RenderBox(val id: String, val boundingBox: BoundingBox, val isNew: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldenSaveScreen(
    onBack: () -> Unit,
    vm: GoldenSaveViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsState()
    val saveState by vm.saveState.collectAsState()

    LaunchedEffect(saveState) {
        if (saveState is GoldenSaveViewModel.SaveState.Saved) onBack()
    }

    val selectedKnown = uiState.knownBoxes.firstOrNull { it.itemId == uiState.selectedId }
    val selectedNew = uiState.newBoxes.firstOrNull { it.localId == uiState.selectedId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when {
                        selectedNew != null -> OutlinedTextField(
                            value = selectedNew.name,
                            onValueChange = { vm.updateNewBoxName(selectedNew.localId, it) },
                            placeholder = { Text("Item name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        selectedKnown != null -> Text(selectedKnown.name)
                        else -> Text("")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        vm.confirmSelection()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    when {
                        selectedKnown != null -> {
                            if (selectedKnown.isTransparentContainer) {
                                FillStates.forEach { state ->
                                    FillStateChip(
                                        state = state,
                                        selected = selectedKnown.fillState == state,
                                        onClick = { vm.setFillState(selectedKnown.itemId, state) }
                                    )
                                }
                            }
                            IconButton(onClick = vm::confirmSelection) {
                                Icon(Icons.Default.Check, contentDescription = "Done")
                            }
                            IconButton(onClick = vm::deleteSelected) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Mark removed",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        selectedNew != null -> {
                            IconButton(onClick = vm::confirmSelection) {
                                Icon(Icons.Default.Check, contentDescription = "Done")
                            }
                            IconButton(onClick = vm::deleteSelected) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Discard",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        else -> {
                            if (saveState is GoldenSaveViewModel.SaveState.Saving) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier
                                        .height(24.dp)
                                        .padding(end = 16.dp)
                                )
                            } else {
                                TextButton(onClick = vm::save) { Text("Save") }
                            }
                        }
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
            val bitmap = uiState.captureData.bitmap
            if (bitmap == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                AnnotationCanvas(
                    bitmap = bitmap,
                    guideLines = uiState.guideLines,
                    knownBoxes = uiState.knownBoxes,
                    newBoxes = uiState.newBoxes,
                    selectedId = uiState.selectedId,
                    onSelect = vm::select,
                    onCreate = vm::createNewBox,
                    onUpdateBoundingBox = vm::updateBoundingBox,
                    onConfirmSelection = vm::confirmSelection,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (saveState is GoldenSaveViewModel.SaveState.Error) {
                Text(
                    text = (saveState as GoldenSaveViewModel.SaveState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun FillStateChip(state: ItemChange, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .clickable(onClick = onClick)
            .background(
                if (selected) state.chipColor else state.chipColor.copy(alpha = 0.35f),
                CircleShape
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(state.symbol, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AnnotationCanvas(
    bitmap: Bitmap,
    guideLines: List<GuideLine>,
    knownBoxes: List<KnownAnnotationBox>,
    newBoxes: List<NewAnnotationBox>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onCreate: (BoundingBox) -> Unit,
    onUpdateBoundingBox: (String, BoundingBox) -> Unit,
    onConfirmSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val renderBoxes = knownBoxes.map { RenderBox(it.itemId, it.boundingBox, isNew = false) } +
        newBoxes.map { RenderBox(it.localId, it.boundingBox, isNew = true) }

    val currentBitmap by rememberUpdatedState(bitmap)
    val currentGuideLines by rememberUpdatedState(guideLines)
    val currentRenderBoxes by rememberUpdatedState(renderBoxes)
    val currentSelectedId by rememberUpdatedState(selectedId)

    Box(modifier = modifier) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    val hitPaddingPx = HitPaddingDp.dp.toPx()
                    awaitEachGesture {
                        val firstDown = awaitFirstDown(requireUnconsumed = false)
                        val gestureSelectedId = currentSelectedId
                        val hitId = hitTest(currentRenderBoxes, firstDown.position, canvasSize, currentBitmap, hitPaddingPx)
                        val isOnSelected = hitId != null && hitId == gestureSelectedId

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
                                        val current = currentRenderBoxes.firstOrNull { it.id == gestureSelectedId }?.boundingBox
                                        if (current != null) {
                                            val moved = current.copy(
                                                x = (current.x + fracDelta.x).coerceIn(0f, 1f - current.width),
                                                y = (current.y + fracDelta.y).coerceIn(0f, 1f - current.height)
                                            )
                                            onUpdateBoundingBox(gestureSelectedId, moved)
                                        }
                                    }
                                }
                            } else if (active.size >= 2 && gestureSelectedId != null) {
                                if (secondPointerId == null) {
                                    val newPointer = active.firstOrNull { it.id != firstDown.id }
                                    val p1 = active.firstOrNull { it.id == firstDown.id }
                                    if (newPointer != null && p1 != null) {
                                        secondPointerId = newPointer.id
                                        resizeStartSeparation = Offset(
                                            abs(p1.position.x - newPointer.position.x),
                                            abs(p1.position.y - newPointer.position.y)
                                        )
                                        gestureBoxStart = currentRenderBoxes.firstOrNull { it.id == gestureSelectedId }?.boundingBox
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
                                        val newWidth = (boxStart.width + fracDelta.x).coerceIn(MinBoxSize, 1f - boxStart.x)
                                        val newHeight = (boxStart.height + fracDelta.y).coerceIn(MinBoxSize, 1f - boxStart.y)
                                        onUpdateBoundingBox(gestureSelectedId, boxStart.copy(width = newWidth, height = newHeight))
                                    }
                                }
                            }
                        }

                        val wasTap = secondPointerId == null && totalDrag.getDistance() <= TapSlopPx
                        if (wasTap) {
                            if (gestureSelectedId != null && gestureSelectedId != hitId) {
                                onConfirmSelection()
                            }
                            if (hitId != null) {
                                if (hitId != gestureSelectedId) onSelect(hitId)
                            } else {
                                val box = computeDefaultBox(firstDown.position, canvasSize, currentBitmap, currentGuideLines)
                                onCreate(box)
                            }
                        }
                    }
                }
        ) {
            val strokeWidth = 2.dp.toPx()
            guideLines.forEach { line ->
                if (line.isHorizontal) {
                    val y = line.position * size.height
                    drawLine(Color.Yellow.copy(alpha = 0.35f), Offset(0f, y), Offset(size.width, y), strokeWidth)
                } else {
                    val x = line.position * size.width
                    drawLine(Color.Yellow.copy(alpha = 0.35f), Offset(x, 0f), Offset(x, size.height), strokeWidth)
                }
            }
            renderBoxes.forEach { box ->
                val rect = bitmapFractionToCanvasRect(box.boundingBox, canvasSize, bitmap)
                val color = when {
                    box.id == selectedId -> SelectedBoxColor
                    box.isNew -> NewBoxColor
                    else -> KnownBoxColor
                }
                drawRect(color, topLeft = rect.topLeft, size = rect.size, style = Stroke(width = strokeWidth))
            }
        }

        if (canvasSize != IntSize.Zero) {
            (knownBoxes.map { it.itemId to it.name } + newBoxes.map { it.localId to it.name.ifBlank { "…" } })
                .forEach { (id, label) ->
                    val box = renderBoxes.firstOrNull { it.id == id } ?: return@forEach
                    val rect = bitmapFractionToCanvasRect(box.boundingBox, canvasSize, bitmap)
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .offset { IntOffset(rect.left.roundToInt(), (rect.top - 20f).roundToInt()) }
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
        }
    }
}

private fun hitTest(
    boxes: List<RenderBox>,
    position: Offset,
    canvasSize: IntSize,
    bitmap: Bitmap,
    paddingPx: Float
): String? {
    if (canvasSize == IntSize.Zero) return null
    return boxes.lastOrNull { box ->
        val rect = bitmapFractionToCanvasRect(box.boundingBox, canvasSize, bitmap)
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
