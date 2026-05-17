package com.jugurdzija.homeshelf.ui.detail

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jugurdzija.homeshelf.data.GuideLine
import kotlin.math.abs

private val GuideLineYellow = Color(0xFFFFEB3B)
private val GuideLineRed = Color(0xFFE53935)
private const val DragThreshold = 40f

private fun decodeWithExifRotation(filePath: String): Bitmap? {
    val bitmap = BitmapFactory.decodeFile(filePath) ?: return null
    val exif = ExifInterface(filePath)
    val rotation = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (rotation == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(rotation) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

@Composable
fun ImageDetailScreen(filePath: String, onBack: () -> Unit) {
    val bitmap = remember(filePath) { decodeWithExifRotation(filePath) }
    val vm: ImageDetailViewModel = hiltViewModel()
    FileImageDetailContent(bitmap = bitmap, onBack = onBack, vm = vm)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileImageDetailContent(bitmap: Bitmap?, onBack: () -> Unit, vm: ImageDetailViewModel) {
    val processState by vm.processState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedId by remember { mutableStateOf(-1) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(processState) {
        val msg = when (val s = processState) {
            is GridProcessState.Done -> "${s.cellCount} cells extracted and saved"
            is GridProcessState.NoCells -> "No cells found — need ≥2 H and ≥2 V lines"
            is GridProcessState.Error -> "Error: ${s.message}"
            else -> null
        }
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            vm.resetProcessState()
        }
    }

    val hCount = vm.guideLines.count { it.isHorizontal }
    val vCount = vm.guideLines.count { !it.isHorizontal }
    val canProcess = bitmap != null &&
            canvasSize != IntSize.Zero &&
            processState !is GridProcessState.Processing &&
            hCount >= 2 && vCount >= 2

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
                        onClick = { vm.processGrid(canvasSize.width, canvasSize.height, bitmap!!) },
                        enabled = canProcess
                    ) { Text("Process") }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
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

                if (processState is GridProcessState.Processing) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                Text(
                    text = "${bitmap.width} × ${bitmap.height} px",
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
            } else {
                Text(
                    text = "Could not load image.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
