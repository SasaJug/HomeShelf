package com.jugurdzija.homeshelf.ui.reference

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.graphics.createBitmap
import com.jugurdzija.homeshelf.ui.theme.HomeShelfTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceDetailScreen(
    onBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onMarkItems: (String) -> Unit,
    onRescan: (String) -> Unit,
    onDeleted: () -> Unit,
    onCaptureTestPhoto: ((String) -> Unit)? = null,
    onViewHistory: ((String) -> Unit)? = null,
    vm: ReferenceDetailViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val showDeleteConfirmation by vm.showDeleteConfirmation.collectAsState()

    LaunchedEffect(vm.navEvent) {
        vm.navEvent.collect { event ->
            when (event) {
                ReferenceDetailNavEvent.Deleted -> onDeleted()
            }
        }
    }

    ReferenceDetailScreenContent(
        state = state,
        showDeleteConfirmation = showDeleteConfirmation,
        onBack = onBack,
        onNavigateToEdit = onNavigateToEdit,
        onMarkItems = onMarkItems,
        onRescan = onRescan,
        onRequestDelete = vm::requestDelete,
        onCancelDelete = vm::cancelDelete,
        onConfirmDelete = vm::confirmDelete,
        onRename = vm::renameStorage,
        onCaptureTestPhoto = onCaptureTestPhoto,
        onViewHistory = onViewHistory
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReferenceDetailScreenContent(
    state: ReferenceDetailUiState,
    showDeleteConfirmation: Boolean,
    onBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onMarkItems: (String) -> Unit,
    onRescan: (String) -> Unit,
    onRequestDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onRename: (String) -> Unit = {},
    onCaptureTestPhoto: ((String) -> Unit)? = null,
    onViewHistory: ((String) -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val storageName = (state as? ReferenceDetailUiState.Loaded)?.storageName
                    if (isEditingName && storageName != null) {
                        OutlinedTextField(
                            value = nameDraft,
                            onValueChange = { nameDraft = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row {
                            Text(storageName ?: "")
                            if (storageName != null) {
                                IconButton(onClick = {
                                    nameDraft = storageName
                                    isEditingName = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Rename storage")
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditingName) {
                        IconButton(onClick = {
                            onRename(nameDraft)
                            isEditingName = false
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save name")
                        }
                    } else if (state is ReferenceDetailUiState.Loaded) {
                        BadgedBox(badge = { if (!state.hasGuideLines) Badge() }) {
                            IconButton(onClick = { onNavigateToEdit(state.storageId) }) {
                                Icon(
                                    Icons.Default.GridOn,
                                    contentDescription = if (state.hasGuideLines) {
                                        "Edit Guidelines"
                                    } else {
                                        "Edit Guidelines (no grid set)"
                                    }
                                )
                            }
                        }
                        BadgedBox(badge = { if (!state.hasMarkedItems) Badge() }) {
                            IconButton(onClick = { onMarkItems(state.storageId) }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Label,
                                    contentDescription = if (state.hasMarkedItems) {
                                        "Mark Items"
                                    } else {
                                        "Mark Items (no items marked)"
                                    }
                                )
                            }
                        }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More actions")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Replace Photo") },
                                    leadingIcon = { Icon(Icons.Default.Autorenew, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onRescan(state.storageId)
                                    }
                                )
                                if (onCaptureTestPhoto != null) {
                                    DropdownMenuItem(
                                        text = { Text("Capture Test Photo") },
                                        leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            onCaptureTestPhoto(state.storageId)
                                        }
                                    )
                                }
                                if (onViewHistory != null) {
                                    DropdownMenuItem(
                                        text = { Text("View History") },
                                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                                        onClick = {
                                            menuExpanded = false
                                            onViewHistory(state.storageId)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onRequestDelete()
                                    }
                                )
                            }
                        }
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
            when (state) {
                ReferenceDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ReferenceDetailUiState.Loaded -> {
                    Image(
                        bitmap = state.bitmap.asImageBitmap(),
                        contentDescription = state.storageName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                is ReferenceDetailUiState.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            title = { Text("Delete storage?") },
            text = { Text("This permanently removes the storage and its reference photo.") },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelDelete) { Text("Cancel") }
            }
        )
    }
}

private fun previewBitmap(): Bitmap {
    val bitmap = createBitmap(360, 480)
    android.graphics.Canvas(bitmap).drawColor(android.graphics.Color.DKGRAY)
    return bitmap
}

@Preview(showBackground = true)
@Composable
private fun ReferenceDetailScreenLoadingPreview() {
    HomeShelfTheme {
        ReferenceDetailScreenContent(
            state = ReferenceDetailUiState.Loading,
            showDeleteConfirmation = false,
            onBack = {},
            onNavigateToEdit = {},
            onMarkItems = {},
            onRescan = {},
            onRequestDelete = {},
            onCancelDelete = {},
            onConfirmDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReferenceDetailScreenLoadedPreview() {
    HomeShelfTheme {
        ReferenceDetailScreenContent(
            state = ReferenceDetailUiState.Loaded(
                storageId = "1",
                storageName = "Pantry Shelf A",
                bitmap = previewBitmap(),
                hasGuideLines = true,
                hasMarkedItems = true
            ),
            showDeleteConfirmation = false,
            onBack = {},
            onNavigateToEdit = {},
            onMarkItems = {},
            onRescan = {},
            onRequestDelete = {},
            onCancelDelete = {},
            onConfirmDelete = {},
            onCaptureTestPhoto = {},
            onViewHistory = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReferenceDetailScreenDeleteConfirmationPreview() {
    HomeShelfTheme {
        ReferenceDetailScreenContent(
            state = ReferenceDetailUiState.Loaded(
                storageId = "1",
                storageName = "Pantry Shelf A",
                bitmap = previewBitmap(),
                hasGuideLines = true,
                hasMarkedItems = true
            ),
            showDeleteConfirmation = true,
            onBack = {},
            onNavigateToEdit = {},
            onMarkItems = {},
            onRescan = {},
            onRequestDelete = {},
            onCancelDelete = {},
            onConfirmDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReferenceDetailScreenErrorPreview() {
    HomeShelfTheme {
        ReferenceDetailScreenContent(
            state = ReferenceDetailUiState.Error("Storage not found"),
            showDeleteConfirmation = false,
            onBack = {},
            onNavigateToEdit = {},
            onMarkItems = {},
            onRescan = {},
            onRequestDelete = {},
            onCancelDelete = {},
            onConfirmDelete = {}
        )
    }
}
