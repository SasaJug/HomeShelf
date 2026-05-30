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
import androidx.compose.material.icons.filled.Delete
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
import com.jugurdzija.homeshelf.data.ReferenceItem
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceScreen(
    onNavigateToCapture: () -> Unit,
    onNavigateToCompare: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToManage: (() -> Unit)? = null,
    vm: ReferenceViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val thumbnails by vm.thumbnails.collectAsState()

    ReferenceContent(
        state = state,
        thumbnails = thumbnails,
        onNavigateToCapture = onNavigateToCapture,
        onNavigateToCompare = onNavigateToCompare,
        onNavigateToDetail = onNavigateToDetail,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToManage = onNavigateToManage,
        onDelete = vm::onDelete
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReferenceContent(
    state: ReferenceListUiState,
    thumbnails: Map<String, Bitmap>,
    onNavigateToCapture: () -> Unit,
    onNavigateToCompare: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToManage: (() -> Unit)?,
    onDelete: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("References") },
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
                            "No reference images yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap Add Reference to capture one.",
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
                        contentPadding = PaddingValues(bottom = 148.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            ReferenceListItem(
                                item = item,
                                thumbnail = thumbnails[item.id],
                                onDelete = { onDelete(item.id) },
                                onClick = { onNavigateToDetail(item.file.absolutePath) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToCapture,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add Reference") }
                Button(
                    onClick = onNavigateToCompare,
                    enabled = state is ReferenceListUiState.Loaded,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Compare") }
                if (onNavigateToManage != null) {
                    Button(
                        onClick = onNavigateToManage,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Golden Captures") }
                }
            }
        }
    }
}

@Composable
private fun ReferenceListItem(
    item: ReferenceItem,
    thumbnail: Bitmap?,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
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
                    contentDescription = item.label,
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
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete ${item.label}",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}


@Preview(showBackground = true, name = "Loading")
@Composable
private fun ReferenceScreenLoadingPreview() {
    ReferenceContent(
        state = ReferenceListUiState.Loading,
        thumbnails = emptyMap(),
        onNavigateToCapture = {},
        onNavigateToCompare = {},
        onNavigateToDetail = {},
        onNavigateToSettings = {},
        onNavigateToManage = null,
        onDelete = {}
    )
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun ReferenceScreenEmptyPreview() {
    ReferenceContent(
        state = ReferenceListUiState.Empty,
        thumbnails = emptyMap(),
        onNavigateToCapture = {},
        onNavigateToCompare = {},
        onNavigateToDetail = {},
        onNavigateToSettings = {},
        onNavigateToManage = null,
        onDelete = {}
    )
}

@Preview(showBackground = true, name = "Loaded")
@Composable
private fun ReferenceScreenLoadedPreview() {
    val items = listOf(
        ReferenceItem(id = "1", label = "Front shelf", file = File("front.jpg")),
        ReferenceItem(id = "2", label = "Back shelf", file = File("back.jpg")),
        ReferenceItem(id = "3", label = "Side shelf", file = File("side.jpg")),
    )
    ReferenceContent(
        state = ReferenceListUiState.Loaded(items),
        thumbnails = emptyMap(),
        onNavigateToCapture = {},
        onNavigateToCompare = {},
        onNavigateToDetail = {},
        onNavigateToSettings = {},
        onNavigateToManage = {},
        onDelete = {}
    )
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun ReferenceScreenErrorPreview() {
    val items = listOf(
        ReferenceItem(id = "1", label = "Front shelf", file = File("front.jpg")),
    )
    ReferenceContent(
        state = ReferenceListUiState.Error("Failed to load thumbnail", items),
        thumbnails = emptyMap(),
        onNavigateToCapture = {},
        onNavigateToCompare = {},
        onNavigateToDetail = {},
        onNavigateToSettings = {},
        onNavigateToManage = null,
        onDelete = {}
    )
}

