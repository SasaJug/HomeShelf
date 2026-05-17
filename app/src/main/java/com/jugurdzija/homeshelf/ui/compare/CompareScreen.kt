package com.jugurdzija.homeshelf.ui.compare

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jugurdzija.homeshelf.embedding.ReferenceMatch
import com.jugurdzija.homeshelf.ui.common.CameraPermissionGate
import com.jugurdzija.homeshelf.ui.common.FrameThrottlingAnalyzer
import com.jugurdzija.homeshelf.ui.common.bindCameraX
import com.jugurdzija.homeshelf.ui.detail.BitmapDetailHolder
import org.opencv.android.OpenCVLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    onBack: () -> Unit,
    onNavigateToDetail: () -> Unit,
) {
    val vm: CompareViewModel = hiltViewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        OpenCVLoader.initLocal()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compare") },
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
            when (val s = state) {
                is CompareUiState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                is CompareUiState.MissingReference -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = s.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onBack) { Text("Go Back") }
                    }
                }

                is CompareUiState.PermissionDenied -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Camera permission is required to compare images.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            )
                        }) { Text("Open Settings") }
                    }
                }

                is CompareUiState.Streaming, is CompareUiState.Error, is CompareUiState.CapturePending, is CompareUiState.Captured -> {
                    CameraPermissionGate(onDenied = { vm.onPermissionDenied() }) {
                        val previewView = remember { PreviewView(context) }

                        LaunchedEffect(previewView) {
                            bindCameraX(
                                context = context,
                                lifecycleOwner = lifecycleOwner,
                                previewView = previewView,
                                analyzer = FrameThrottlingAnalyzer(skipFactor = 15) { bitmap ->
                                    vm.onFrameReceived(bitmap)
                                }
                            )
                        }

                        if (s is CompareUiState.CapturePending) {
                            LaunchedEffect(Unit) {
                                val bitmap = previewView.bitmap
                                if (bitmap != null) vm.onPreviewBitmapCaptured(bitmap)
                            }
                        }

                        if (s is CompareUiState.Captured) {
                            Image(
                                bitmap = s.frameBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (s.guideLines.isNotEmpty()) {
                                GuideLineOverlay(guideLines = s.guideLines, modifier = Modifier.fillMaxSize())
                            }
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Aligning…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        } else {
                            AndroidView(
                                factory = { previewView },
                                modifier = Modifier.fillMaxSize()
                            )

                            val guideLines = (s as? CompareUiState.Streaming)?.guideLines
                                ?: (s as? CompareUiState.Error)?.guideLines
                                ?: (s as? CompareUiState.CapturePending)?.guideLines
                                ?: emptyList()

                            if (guideLines.isNotEmpty()) {
                                GuideLineOverlay(
                                    guideLines = guideLines,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        val matches = (s as? CompareUiState.Streaming)?.matches
                            ?: (s as? CompareUiState.Error)?.matches
                            ?: (s as? CompareUiState.CapturePending)?.matches
                            ?: (s as? CompareUiState.Captured)?.matches
                            ?: emptyList()

                        MatchesOverlay(
                            matches = matches,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(16.dp)
                        )

                        if (s is CompareUiState.Error) {
                            Text(
                                text = s.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                is CompareUiState.Aligned -> {
                    Image(
                        bitmap = s.alignedBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    if (s.guideLines.isNotEmpty()) {
                        GuideLineOverlay(guideLines = s.guideLines, modifier = Modifier.fillMaxSize())
                    }

                    MatchesOverlay(
                        matches = s.matches,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 148.dp)
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                BitmapDetailHolder.pending = s.alignedBitmap
                                BitmapDetailHolder.pendingGuideLines = s.guideLines
                                BitmapDetailHolder.pendingReferenceFilePath = s.referenceFilePath
                                onNavigateToDetail()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View Result")
                        }
                        Button(
                            onClick = { vm.onScanAgain() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Scan Again")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchesOverlay(
    matches: List<ReferenceMatch>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (matches.isEmpty()) {
                Text(
                    text = "Scanning…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                matches.forEachIndexed { index, match ->
                    if (index > 0) Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = match.item.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${"%.1f".format(match.similarity * 100)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                            color = similarityColor(match.similarity)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun similarityColor(similarity: Double) = when {
    similarity >= 0.9 -> MaterialTheme.colorScheme.primary
    similarity >= 0.7 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}

@Composable
private fun GuideLineOverlay(guideLines: List<com.jugurdzija.homeshelf.data.GuideLine>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
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
}
