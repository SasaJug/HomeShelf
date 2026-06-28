package com.jugurdzija.homeshelf.ui.golden

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jugurdzija.homeshelf.ui.common.CameraPermissionGate
import com.jugurdzija.homeshelf.ui.common.CameraPreview
import com.jugurdzija.homeshelf.ui.common.GuideLineOverlay
import com.jugurdzija.homeshelf.ui.common.LevelIndicatorOverlay
import com.jugurdzija.homeshelf.ui.common.rememberDeviceOrientation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldenCaptureScreen(
    onBack: () -> Unit,
    onNavigateToSave: () -> Unit,
    vm: GoldenCaptureViewModel = hiltViewModel()
) {
    val guideLineState by vm.guideLineState.collectAsState()
    val guideLines = (guideLineState as? GoldenCaptureViewModel.GuideLineState.Ready)?.guideLines ?: emptyList()
    val canCapture = guideLineState is GoldenCaptureViewModel.GuideLineState.Ready
    val orientationState = rememberDeviceOrientation()
    val orientation by orientationState
    var isCapturing by remember { mutableStateOf(false) }
    var captureTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(vm.navigateToSave) {
        vm.navigateToSave.collect { onNavigateToSave() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capture Test Photo") },
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
            CameraPermissionGate {
                CameraPreview(
                    captureKey = if (captureTriggered) true else null,
                    onBitmapCaptured = { bitmap ->
                        if (bitmap != null) {
                            vm.onCaptureBitmap(bitmap)
                        } else {
                            isCapturing = false
                            captureTriggered = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (guideLines.isNotEmpty()) {
                    GuideLineOverlay(guideLines = guideLines, modifier = Modifier.fillMaxSize())
                }

                LevelIndicatorOverlay(
                    orientation = orientation,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                )

                if (guideLineState is GoldenCaptureViewModel.GuideLineState.Unavailable) {
                    Text(
                        "This storage has no guide lines set up yet. Add guide lines before capturing test photos.",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(16.dp)
                    )
                }

                Button(
                    onClick = {
                        isCapturing = true
                        captureTriggered = true
                    },
                    enabled = !isCapturing && canCapture,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .size(72.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}
