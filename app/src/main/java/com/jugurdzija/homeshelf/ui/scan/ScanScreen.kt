package com.jugurdzija.homeshelf.ui.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.StorageItem
import com.jugurdzija.homeshelf.ui.common.CameraPermissionGate
import com.jugurdzija.homeshelf.ui.common.CameraPreview
import com.jugurdzija.homeshelf.ui.common.DetectionBadge
import com.jugurdzija.homeshelf.ui.common.DeviceOrientation
import com.jugurdzija.homeshelf.ui.common.GuideLineOverlay
import com.jugurdzija.homeshelf.ui.common.LevelIndicatorOverlay
import com.jugurdzija.homeshelf.ui.common.rememberDeviceOrientation
import com.jugurdzija.homeshelf.ui.theme.HomeShelfTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onNavigateToReview: (String) -> Unit,
    onNavigateToConfirm: (String?) -> Unit,
    vm: ScanViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val orientationState = rememberDeviceOrientation()
    val orientation by orientationState
    var isCapturing by remember { mutableStateOf(false) }

    LaunchedEffect(vm.navEvent) {
        vm.navEvent.collect { event ->
            isCapturing = false
            when (event) {
                is ScanNavEvent.ToReview -> onNavigateToReview(event.storageId)
                is ScanNavEvent.ToConfirm -> onNavigateToConfirm(event.storageId)
            }
        }
    }

    LaunchedEffect(state) {
        if (state is ScanUiState.Error) {
            isCapturing = false
        }
    }

    val onCaptureStateChange: (capturing: Boolean) -> Unit = { capturing ->
        isCapturing = capturing
    }

    ScanScreenContent(
        state = state,
        orientation = orientation,
        isCapturing = isCapturing,
        onBack = onBack,
        onCaptureClick = onCaptureStateChange
    ) {
        CameraPermissionGate(onDenied = { vm.onPermissionDenied() }) {
            CameraPreview(
                onFrameReceived = vm::onFrameReceived,
                captureKey = if (isCapturing) true else null,
                onBitmapCaptured = { bitmap ->
                    if (bitmap != null) {
                        vm.onCaptureBitmap(bitmap)
                    } else {
                        onCaptureStateChange(false)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanScreenContent(
    state: ScanUiState,
    orientation: DeviceOrientation,
    isCapturing: Boolean,
    onBack: () -> Unit,
    onCaptureClick: (capturing: Boolean) -> Unit,
    cameraPreview: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Storage") },
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
            cameraPreview()

            val guideLines = (state as? ScanUiState.Streaming)?.guideLines
                ?: (state as? ScanUiState.Error)?.guideLines
                ?: emptyList()

            if (guideLines.isNotEmpty()) {
                GuideLineOverlay(guideLines = guideLines, modifier = Modifier.fillMaxSize())
            }

            LevelIndicatorOverlay(
                orientation = orientation,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )

            val detected = (state as? ScanUiState.Streaming)?.detected
            DetectionBadge(
                detectedName = detected?.name,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
            )

            if (state is ScanUiState.Error) {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Button(
                onClick = { onCaptureClick(true) },
                enabled = !isCapturing,
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

@Composable
private fun PreviewCameraPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B1B1B))
    )
}

private val previewStorageItem = StorageItem(
    id = "1",
    name = "Pantry Shelf A",
    createdAt = 0L,
    updatedAt = 1_700_000_000_000L
)

private val previewGuideLines = listOf(
    GuideLine(id = 0, isHorizontal = true, position = 0.33f),
    GuideLine(id = 1, isHorizontal = true, position = 0.66f),
    GuideLine(id = 2, isHorizontal = false, position = 0.5f)
)

@Preview(showBackground = true)
@Composable
private fun ScanScreenStreamingPreview() {
    HomeShelfTheme {
        ScanScreenContent(
            state = ScanUiState.Streaming(),
            orientation = DeviceOrientation(),
            isCapturing = false,
            onBack = {},
            onCaptureClick = {},
            cameraPreview = { PreviewCameraPlaceholder() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanScreenDetectedPreview() {
    HomeShelfTheme {
        ScanScreenContent(
            state = ScanUiState.Streaming(detected = previewStorageItem, guideLines = previewGuideLines),
            orientation = DeviceOrientation(pitch = 1f, roll = -2f),
            isCapturing = false,
            onBack = {},
            onCaptureClick = {},
            cameraPreview = { PreviewCameraPlaceholder() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanScreenCapturingPreview() {
    HomeShelfTheme {
        ScanScreenContent(
            state = ScanUiState.Streaming(detected = previewStorageItem, guideLines = previewGuideLines),
            orientation = DeviceOrientation(),
            isCapturing = true,
            onBack = {},
            onCaptureClick = {},
            cameraPreview = { PreviewCameraPlaceholder() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanScreenErrorPreview() {
    HomeShelfTheme {
        ScanScreenContent(
            state = ScanUiState.Error(
                message = "Camera permission is required",
                detected = null,
                guideLines = emptyList()
            ),
            orientation = DeviceOrientation(),
            isCapturing = false,
            onBack = {},
            onCaptureClick = {},
            cameraPreview = { PreviewCameraPlaceholder() }
        )
    }
}
