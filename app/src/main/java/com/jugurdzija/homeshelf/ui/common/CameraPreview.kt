package com.jugurdzija.homeshelf.ui.common

import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

// previewView.bitmap dimensions equal this composable's layout size. All callers that
// compare bitmaps against each other must render CameraPreview at the same layout size.
@Composable
fun CameraPreview(
    showPreview: Boolean = true,
    onFrameReceived: ((Bitmap) -> Unit)? = null,
    skipFactor: Int = 15,
    captureKey: Any? = null,
    onBitmapCaptured: (Bitmap?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(previewView) {
        if (onFrameReceived != null) {
            bindCameraX(
                context = context,
                lifecycleOwner = lifecycleOwner,
                previewView = previewView,
                analyzer = FrameThrottlingAnalyzer(skipFactor = skipFactor) { bitmap ->
                    onFrameReceived(bitmap)
                }
            )
        } else {
            bindCameraXPreview(
                context = context,
                lifecycleOwner = lifecycleOwner,
                previewView = previewView
            )
        }
    }

    LaunchedEffect(captureKey) {
        if (captureKey != null) onBitmapCaptured(previewView.bitmap)
    }

    if (showPreview) {
        AndroidView(factory = { previewView }, modifier = modifier)
    }
}
