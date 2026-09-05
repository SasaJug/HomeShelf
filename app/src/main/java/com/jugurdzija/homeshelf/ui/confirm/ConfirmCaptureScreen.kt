package com.jugurdzija.homeshelf.ui.confirm

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jugurdzija.homeshelf.ui.theme.HomeShelfTheme
import com.jugurdzija.homeshelf.usecase.StorageSaveResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmCaptureScreen(
    onSaved: (storageId: String) -> Unit,
    onDiscarded: () -> Unit,
    vm: ConfirmCaptureViewModel = hiltViewModel()
) {
    val bitmap by vm.bitmapState.collectAsState()
    val existingName by vm.existingName.collectAsState()
    val saveState by vm.saveState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(vm.navEvent) {
        vm.navEvent.collect { event ->
            when (event) {
                is ConfirmCaptureNavEvent.Saved -> onSaved(event.storageId)
                is ConfirmCaptureNavEvent.Discarded -> onDiscarded()
            }
        }
    }

    LaunchedEffect(saveState) {
        val result = saveState
        if (result is StorageSaveResult.Error) {
            snackbarHostState.showSnackbar(result.message)
            vm.resetSaveState()
        }
    }

    ConfirmCaptureScreenContent(
        bitmap = bitmap,
        title = if (vm.isRescan) "Confirm Replace Photo: ${existingName.orEmpty()}" else "Confirm New Storage",
        isRescan = vm.isRescan,
        name = vm.name,
        onNameChange = { vm.name = it },
        onBack = onDiscarded,
        onSave = vm::save,
        onDiscard = vm::discard,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmCaptureScreenContent(
    bitmap: Bitmap?,
    title: String,
    isRescan: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Captured photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            if (!isRescan) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Storage name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f)) {
                    Text("Discard")
                }
                Button(
                    onClick = onSave,
                    enabled = bitmap != null && (isRescan || name.isNotBlank()),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
        }
    }
}

private val previewBitmap: Bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)

@Preview(showBackground = true)
@Composable
private fun ConfirmCaptureNewStoragePreview() {
    HomeShelfTheme {
        ConfirmCaptureScreenContent(
            bitmap = previewBitmap,
            title = "Confirm New Storage",
            isRescan = false,
            name = "",
            onNameChange = {},
            onBack = {},
            onSave = {},
            onDiscard = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmCaptureRescanPreview() {
    HomeShelfTheme {
        ConfirmCaptureScreenContent(
            bitmap = previewBitmap,
            title = "Confirm Replace Photo: Pantry Shelf A",
            isRescan = true,
            name = "",
            onNameChange = {},
            onBack = {},
            onSave = {},
            onDiscard = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
