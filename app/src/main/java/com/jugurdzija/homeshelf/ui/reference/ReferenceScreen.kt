package com.jugurdzija.homeshelf.ui.reference

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jugurdzija.homeshelf.data.StorageItem
import com.jugurdzija.homeshelf.ui.theme.HomeShelfTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceScreen(
    onNavigateToEdit: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToScan: () -> Unit,
    onMarkItems: (String) -> Unit,
    onCaptureTestPhoto: ((String) -> Unit)? = null,
    onViewHistory: ((String) -> Unit)? = null,
    vm: ReferenceViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val thumbnails by vm.thumbnails.collectAsState()

    ReferenceScreenContent(
        state = state,
        thumbnails = thumbnails,
        onDelete = vm::onDelete,
        onNavigateToEdit = onNavigateToEdit,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToScan = onNavigateToScan,
        onMarkItems = onMarkItems,
        onCaptureTestPhoto = onCaptureTestPhoto,
        onViewHistory = onViewHistory
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReferenceScreenContent(
    state: ReferenceListUiState,
    thumbnails: Map<String, Bitmap>,
    onDelete: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToScan: () -> Unit,
    onMarkItems: (String) -> Unit,
    onCaptureTestPhoto: ((String) -> Unit)? = null,
    onViewHistory: ((String) -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storages") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                is ReferenceListUiState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                is ReferenceListUiState.Empty -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "No storages yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap Scan Storage to add one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is ReferenceListUiState.Loaded, is ReferenceListUiState.Error -> {
                    val items = (s as? ReferenceListUiState.Loaded)?.items
                        ?: (s as? ReferenceListUiState.Error)?.items
                        ?: emptyList()

                    if (s is ReferenceListUiState.Error) {
                        Text(
                            text = s.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(8.dp)
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            StorageListItem(
                                item = item,
                                thumbnail = thumbnails[item.id],
                                onDelete = { onDelete(item.id) },
                                onClick = { onNavigateToEdit(item.id) },
                                onMarkItems = { onMarkItems(item.id) },
                                onCaptureTestPhoto = onCaptureTestPhoto?.let { { it(item.id) } },
                                onViewHistory = onViewHistory?.let { { it(item.id) } }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = onNavigateToScan,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Scan Storage") }
            }
        }
    }
}

@Composable
private fun StorageListItem(
    item: StorageItem,
    thumbnail: Bitmap?,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onMarkItems: () -> Unit,
    onCaptureTestPhoto: (() -> Unit)? = null,
    onViewHistory: (() -> Unit)? = null
) {
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = dateFormat.format(Date(item.updatedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onCaptureTestPhoto != null) {
            IconButton(onClick = onCaptureTestPhoto) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Capture test photo for ${item.name}"
                )
            }
        }
        if (onViewHistory != null) {
            IconButton(onClick = onViewHistory) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "View comparison history for ${item.name}"
                )
            }
        }
        IconButton(onClick = onMarkItems) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Label,
                contentDescription = "Mark items in ${item.name}"
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete ${item.name}",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

private val previewItems = listOf(
    StorageItem(id = "1", name = "Pantry Shelf A", createdAt = 0L, updatedAt = 1_700_000_000_000L),
    StorageItem(id = "2", name = "Garage Cabinet", createdAt = 0L, updatedAt = 1_700_100_000_000L),
    StorageItem(id = "3", name = "Bedroom Closet", createdAt = 0L, updatedAt = 1_700_200_000_000L)
)

@Preview(showBackground = true)
@Composable
private fun ReferenceScreenLoadingPreview() {
    HomeShelfTheme {
        ReferenceScreenContent(
            state = ReferenceListUiState.Loading,
            thumbnails = emptyMap(),
            onDelete = {},
            onNavigateToEdit = {},
            onNavigateToSettings = {},
            onNavigateToScan = {},
            onMarkItems = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReferenceScreenEmptyPreview() {
    HomeShelfTheme {
        ReferenceScreenContent(
            state = ReferenceListUiState.Empty,
            thumbnails = emptyMap(),
            onDelete = {},
            onNavigateToEdit = {},
            onNavigateToSettings = {},
            onNavigateToScan = {},
            onMarkItems = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReferenceScreenLoadedPreview() {
    HomeShelfTheme {
        ReferenceScreenContent(
            state = ReferenceListUiState.Loaded(previewItems),
            thumbnails = emptyMap(),
            onDelete = {},
            onNavigateToEdit = {},
            onNavigateToSettings = {},
            onNavigateToScan = {},
            onMarkItems = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ReferenceScreenErrorPreview() {
    HomeShelfTheme {
        ReferenceScreenContent(
            state = ReferenceListUiState.Error(
                message = "Failed to load storages",
                items = previewItems
            ),
            thumbnails = emptyMap(),
            onDelete = {},
            onNavigateToEdit = {},
            onNavigateToSettings = {},
            onNavigateToScan = {},
            onMarkItems = {}
        )
    }
}
