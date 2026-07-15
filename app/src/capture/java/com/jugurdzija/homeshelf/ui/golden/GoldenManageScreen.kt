package com.jugurdzija.homeshelf.ui.golden

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jugurdzija.homeshelf.data.GoldenConstants
import com.jugurdzija.homeshelf.data.GoldenItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldenManageScreen(
    onBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    vm: GoldenManageViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(vm.events) {
        vm.events.collect { event ->
            when (event) {
                is GoldenManageViewModel.Event.NavigateToDetails -> onNavigateToDetails(event.name)
            }
        }
    }

    GoldenManageContent(
        state = state,
        onBack = onBack,
        onItemClick = vm::onItemClick,
        onDelete = vm::onDelete,
        formatTimestamp = vm::formatTimestamp
    )
}

private fun topBarTitle(state: GoldenManageViewModel.State): String = when (state) {
    is GoldenManageViewModel.State.Loaded -> "History: ${state.referenceName}"
    else -> "Comparison History"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoldenManageContent(
    state: GoldenManageViewModel.State,
    onBack: () -> Unit,
    onItemClick: (String) -> Unit,
    onDelete: (String) -> Unit,
    formatTimestamp: (String) -> String
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle(state)) },
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
                is GoldenManageViewModel.State.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                is GoldenManageViewModel.State.Empty -> {
                    Text(
                        "No comparison photos yet for this reference.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is GoldenManageViewModel.State.Loaded -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(s.items, key = { it.name }) { item ->
                            GoldenManageItem(
                                item = item,
                                formattedTimestamp = formatTimestamp(item.timestamp),
                                onClick = { onItemClick(item.name) },
                                onDelete = { onDelete(item.name) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoldenManageItem(
    item: GoldenItem,
    formattedTimestamp: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val thumbnail by produceState<Bitmap?>(null, item.name) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(
                File(item.dir, GoldenConstants.FILE_PHOTO).absolutePath,
                BitmapFactory.Options().apply { inSampleSize = 4 }
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                formattedTimestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
        }
    }
}

private fun previewItem(name: String, label: String) = GoldenItem(
    name = name,
    referenceLabel = label,
    timestamp = "2024-01-15T10:30:00Z",
    dir = File(name)
)

@Preview(showBackground = true, name = "Loading")
@Composable
private fun GoldenManageLoadingPreview() {
    GoldenManageContent(
        state = GoldenManageViewModel.State.Loading,
        onBack = {},
        onItemClick = {},
        onDelete = {},
        formatTimestamp = { "15 Jan 2024, 10:30" }
    )
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun GoldenManageEmptyPreview() {
    GoldenManageContent(
        state = GoldenManageViewModel.State.Empty,
        onBack = {},
        onItemClick = {},
        onDelete = {},
        formatTimestamp = { "15 Jan 2024, 10:30" }
    )
}

@Preview(showBackground = true, name = "Loaded")
@Composable
private fun GoldenManageLoadedPreview() {
    val items = listOf(
        previewItem("capture_001", "Front Shelf"),
        previewItem("capture_002", "Front Shelf")
    )
    GoldenManageContent(
        state = GoldenManageViewModel.State.Loaded("Front Shelf", items),
        onBack = {},
        onItemClick = {},
        onDelete = {},
        formatTimestamp = { "15 Jan 2024, 10:30" }
    )
}
