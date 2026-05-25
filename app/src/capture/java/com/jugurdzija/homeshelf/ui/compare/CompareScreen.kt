package com.jugurdzija.homeshelf.ui.compare

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jugurdzija.homeshelf.ui.common.CAPTURE_SIMILARITY_THRESHOLD
import com.jugurdzija.homeshelf.ui.common.CameraPermissionGate
import com.jugurdzija.homeshelf.ui.common.FrameThrottlingAnalyzer
import com.jugurdzija.homeshelf.ui.common.GuideLineOverlay
import com.jugurdzija.homeshelf.ui.common.MatchesOverlay
import com.jugurdzija.homeshelf.ui.common.bindCameraX
import com.jugurdzija.homeshelf.ui.golden.GoldenCaptureHolder
import org.opencv.android.OpenCVLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    onBack: () -> Unit,
    onNavigateToGoldenSave: () -> Unit,
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

                is CompareUiState.Streaming, is CompareUiState.Error, is CompareUiState.CapturePending -> {
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
                                val top = s.matches.firstOrNull()
                                if (top != null) {
                                    GoldenCaptureHolder.bitmap = s.capturedBitmap
                                    GoldenCaptureHolder.referenceLabel = top.item.label
                                    GoldenCaptureHolder.similarityScore = top.similarity
                                    GoldenCaptureHolder.similarityThreshold = CAPTURE_SIMILARITY_THRESHOLD
                                    GoldenCaptureHolder.allMatchScores = s.matches.associate { it.item.label to it.similarity }
                                    GoldenCaptureHolder.framesAnalyzed = s.framesAnalyzed
                                    GoldenCaptureHolder.captureAttempt = s.captureAttempt
                                    onNavigateToGoldenSave()
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
            }
        }
    }
}

