package com.jugurdzija.homeshelf.ui.golden

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jugurdzija.homeshelf.data.ChangeType
import com.jugurdzija.homeshelf.data.GuideLine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldenSaveScreen(
    onBack: () -> Unit,
    readOnly: Boolean = false,
    vm: GoldenSaveViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsState()
    val saveState by vm.saveState.collectAsState()
    val guideLineState by vm.guideLineState.collectAsState()

    var tappedCellIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        if (!readOnly) vm.loadGuideLines()
    }

    LaunchedEffect(saveState) {
        if (saveState is GoldenSaveViewModel.SaveState.Saved) onBack()
    }

    tappedCellIndex?.let { index ->
        ChangeTypeDialog(
            cellIndex = index,
            current = uiState.annotations[index] ?: ChangeType.NO_CHANGE,
            onSelect = { changeType ->
                vm.setAnnotation(index, changeType)
                tappedCellIndex = null
            },
            onDismiss = { tappedCellIndex = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (readOnly) "Golden Photo" else "Save Golden Photo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Discard")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.captureData.bitmap?.let { bmp ->
                val lines = (guideLineState as? GoldenSaveViewModel.GuideLineState.Ready)?.guideLines
                if (!readOnly && lines != null) {
                    AnnotatableImage(
                        bitmap = bmp,
                        guideLines = lines,
                        annotations = uiState.annotations,
                        onCellTapped = { tappedCellIndex = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                    )
                    Text(
                        "Tap a cell to annotate what changed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                    )
                    if (!readOnly && guideLineState is GoldenSaveViewModel.GuideLineState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            }

            OutlinedTextField(
                value = uiState.name,
                onValueChange = { if (!readOnly) vm.setName(it) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = readOnly
            )

            Spacer(Modifier.height(4.dp))
            Text("Metadata", style = MaterialTheme.typography.titleSmall)

            MetaRow("Reference", uiState.captureData.referenceLabel ?: "-")
            MetaRow("Similarity score", uiState.captureData.similarityScore?.let { "%.4f".format(it) } ?: "-")
            MetaRow("Threshold", uiState.captureData.similarityThreshold?.let { "%.4f".format(it) } ?: "-")
            MetaRow("Frames analyzed", uiState.captureData.framesAnalyzed?.toString() ?: "-")
            MetaRow("Capture attempt", uiState.captureData.captureAttempt?.toString() ?: "-")
            uiState.captureData.bitmap?.let { MetaRow("Image dimensions", "${it.width} × ${it.height}") }

            uiState.captureData.allMatchScores?.takeIf { it.size > 1 }?.let { scores ->
                Spacer(Modifier.height(4.dp))
                Text("All match scores", style = MaterialTheme.typography.titleSmall)
                scores.entries.sortedByDescending { it.value }.forEach { (label, score) ->
                    MetaRow(label, "%.4f".format(score))
                }
            }

            if (readOnly && uiState.captureData.groundTruth.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Ground Truth", style = MaterialTheme.typography.titleSmall)
                uiState.captureData.groundTruth.sortedBy { it.cellIndex }.forEach { cell ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Cell ${cell.cellIndex}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(cell.changeType.chipColor.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    cell.changeType.symbol,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(cell.changeType.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            when (val s = saveState) {
                is GoldenSaveViewModel.SaveState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
                else -> {}
            }

            if (!readOnly) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                        Text("Discard")
                    }
                    Button(
                        onClick = { vm.save() },
                        modifier = Modifier.weight(1f),
                        enabled = saveState !is GoldenSaveViewModel.SaveState.Saving && uiState.name.isNotBlank()
                    ) {
                        if (saveState is GoldenSaveViewModel.SaveState.Saving) CircularProgressIndicator()
                        else Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnotatableImage(
    bitmap: android.graphics.Bitmap,
    guideLines: List<GuideLine>,
    annotations: Map<Int, ChangeType>,
    onCellTapped: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val hLines = remember(guideLines) { guideLines.filter { it.isHorizontal }.sortedBy { it.position } }
    val vLines = remember(guideLines) { guideLines.filter { !it.isHorizontal }.sortedBy { it.position } }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .onSizeChanged { boxSize = it }
            .pointerInput(hLines, vLines, boxSize) {
                if (boxSize == IntSize.Zero || hLines.size < 2 || vLines.size < 2) return@pointerInput
                detectTapGestures { offset ->
                    val fracX = offset.x / boxSize.width
                    val fracY = offset.y / boxSize.height
                    val rowIdx = hLines.indexOfFirst { it.position > fracY } - 1
                    val colIdx = vLines.indexOfFirst { it.position > fracX } - 1
                    val numCols = vLines.size - 1
                    if (rowIdx >= 0 && colIdx >= 0 && colIdx < numCols) {
                        onCellTapped(rowIdx * numCols + colIdx)
                    }
                }
            }
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 2.dp.toPx()
            guideLines.forEach { line ->
                if (line.isHorizontal) {
                    val y = line.position * size.height
                    drawLine(Color.Yellow, Offset(0f, y), Offset(size.width, y), stroke)
                } else {
                    val x = line.position * size.width
                    drawLine(Color.Yellow, Offset(x, 0f), Offset(x, size.height), stroke)
                }
            }
        }

        if (boxSize != IntSize.Zero && hLines.size >= 2 && vLines.size >= 2) {
            val numCols = vLines.size - 1
            val numRows = hLines.size - 1
            for (rowIdx in 0 until numRows) {
                for (colIdx in 0 until numCols) {
                    val index = rowIdx * numCols + colIdx
                    val cx = (vLines[colIdx].position + vLines[colIdx + 1].position) / 2f
                    val cy = (hLines[rowIdx].position + hLines[rowIdx + 1].position) / 2f
                    val xPx = (cx * boxSize.width).toInt()
                    val yPx = (cy * boxSize.height).toInt()
                    val changeType = annotations[index] ?: ChangeType.NO_CHANGE
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(xPx, yPx) }
                            .background(changeType.chipColor.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = changeType.symbol,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangeTypeDialog(
    cellIndex: Int,
    current: ChangeType,
    onSelect: (ChangeType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cell $cellIndex — What changed?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ChangeType.entries.forEach { type ->
                    TextButton(
                        onClick = { onSelect(type) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${type.symbol}  ${type.label}",
                            fontWeight = if (type == current) FontWeight.Bold else FontWeight.Normal,
                            color = if (type == current) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private val ChangeType.symbol: String
    get() = when (this) {
        ChangeType.NO_CHANGE -> "○"
        ChangeType.ITEM_ADDED -> "+"
        ChangeType.ITEM_REMOVED -> "−"
        ChangeType.ITEM_REPLACED -> "≠"
    }

private val ChangeType.label: String
    get() = when (this) {
        ChangeType.NO_CHANGE -> "No change"
        ChangeType.ITEM_ADDED -> "Item added"
        ChangeType.ITEM_REMOVED -> "Item removed"
        ChangeType.ITEM_REPLACED -> "Item replaced"
    }

private val ChangeType.chipColor: Color
    get() = when (this) {
        ChangeType.NO_CHANGE -> Color(0xFF388E3C)
        ChangeType.ITEM_ADDED -> Color(0xFF1565C0)
        ChangeType.ITEM_REMOVED -> Color(0xFFC62828)
        ChangeType.ITEM_REPLACED -> Color(0xFFE65100)
    }

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
