package com.jugurdzija.homeshelf.ui.common

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

class CameraCaptureController(private val onLaunch: () -> Unit) {
    fun launch() = onLaunch()
}

@Composable
fun rememberCameraCaptureLauncher(onCaptured: (Uri) -> Unit): CameraCaptureController {
    val context = LocalContext.current
    val pendingUri = remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingUri.value?.let(onCaptured)
    }

    return CameraCaptureController {
        val captureDir = File(context.filesDir, "captures").apply { mkdirs() }
        val file = File(captureDir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingUri.value = uri
        launcher.launch(uri)
    }
}
