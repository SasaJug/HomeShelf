package com.jugurdzija.homeshelf.ui.detail

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
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jugurdzija.homeshelf.data.GuideLine

private val SimilarityGreen = Color(0xFF4CAF50)
private val SimilarityYellow = Color(0xFFFFEB3B)
private val SimilarityRed = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlignedDetailScreen(onBack: () -> Unit) {
    val vm: AlignedDetailViewModel = hiltViewModel()
    val capturedBitmap = BitmapDetailHolder.capturedBitmap ?: run { onBack(); return }
    val state by vm.state.collectAsState()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(Unit) {
        vm.analyze()
    }

    val displayBitmap = if (state is AlignedDetailState.Done) {
        (state as AlignedDetailState.Done).alignedBitmap
    } else {
        capturedBitmap
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analysis") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            Image(
                bitmap = displayBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = it },
                contentScale = ContentScale.Crop
            )

            if (state is AlignedDetailState.Done) {
                val done = state as AlignedDetailState.Done
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 2.dp.toPx()
                    done.guideLines.forEach { line ->
                        if (line.isHorizontal) {
                            val y = line.position * size.height
                            drawLine(Color.Yellow, Offset(0f, y), Offset(size.width, y), stroke)
                        } else {
                            val x = line.position * size.width
                            drawLine(Color.Yellow, Offset(x, 0f), Offset(x, size.height), stroke)
                        }
                    }
                }
                val hLines = remember(done.guideLines) { done.guideLines.filter { it.isHorizontal }.sortedBy { it.position } }
                val vLines = remember(done.guideLines) { done.guideLines.filter { !it.isHorizontal }.sortedBy { it.position } }
                SimilarityOverlay(
                    similarities = done.similarities,
                    hLines = hLines,
                    vLines = vLines,
                    canvasSize = canvasSize,
                    modifier = Modifier.fillMaxSize()
                )
            }

            when (state) {
                is AlignedDetailState.Processing -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is AlignedDetailState.AlignmentFailed -> {
                    Text(
                        text = "Alignment failed. Try holding the camera steady.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is AlignedDetailState.NoReference -> {
                    Text(
                        text = "No reference embeddings found.\nProcess the reference image grid first.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is AlignedDetailState.NoCells -> {
                    Text(
                        text = "No grid cells found.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is AlignedDetailState.Error -> {
                    Text(
                        text = (state as AlignedDetailState.Error).message,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {}
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
