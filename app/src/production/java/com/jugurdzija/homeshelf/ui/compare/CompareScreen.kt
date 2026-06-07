package com.jugurdzija.homeshelf.ui.compare

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jugurdzija.homeshelf.ui.common.CameraPermissionGate
import com.jugurdzija.homeshelf.ui.common.CameraPreview
import com.jugurdzija.homeshelf.ui.common.GuideLineOverlay
import com.jugurdzija.homeshelf.ui.common.MatchesOverlay
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
                        CameraPreview(
                            showPreview = true,
                            onFrameReceived = vm::onFrameReceived,
                            captureKey = s as? CompareUiState.CapturePending,
                            onBitmapCaptured = { bitmap ->
                                if (bitmap != null) vm.onPreviewBitmapCaptured(bitmap)
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        val guideLines = (s as? CompareUiState.Streaming)?.guideLines
                            ?: (s as? CompareUiState.Error)?.guideLines
                            ?: (s as? CompareUiState.CapturePending)?.guideLines
                            ?: emptyList()

                        if (guideLines.isNotEmpty()) {
                            GuideLineOverlay(guideLines = guideLines, modifier = Modifier.fillMaxSize())
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

                is CompareUiState.Aligned -> {
                    Image(
                        bitmap = s.capturedBitmap.asImageBitmap(),
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
                            onClick = { onNavigateToDetail() },
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
