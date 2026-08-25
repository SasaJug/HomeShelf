package com.jugurdzija.homeshelf.ui.common

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.jugurdzija.homeshelf.stt.VoiceInputState

@Composable
fun VoiceInputIcon(
    state: VoiceInputState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) onStart() }

    IconButton(
        modifier = modifier,
        enabled = state != VoiceInputState.PROCESSING,
        onClick = {
            when (state) {
                VoiceInputState.RECORDING -> onStop()
                VoiceInputState.PROCESSING -> Unit
                VoiceInputState.IDLE -> {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) onStart() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    ) {
        when (state) {
            VoiceInputState.PROCESSING -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            VoiceInputState.RECORDING -> Icon(
                Icons.Default.Check,
                contentDescription = "Stop recording",
                tint = MaterialTheme.colorScheme.primary
            )
            VoiceInputState.IDLE -> Icon(Icons.Default.Mic, contentDescription = "Voice input")
        }
    }
}
