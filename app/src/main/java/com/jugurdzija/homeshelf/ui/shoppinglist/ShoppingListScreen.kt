package com.jugurdzija.homeshelf.ui.shoppinglist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import com.jugurdzija.homeshelf.data.ShoppingListItem
import com.jugurdzija.homeshelf.ui.common.LifecycleEvents
import com.jugurdzija.homeshelf.ui.theme.HomeShelfTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    onBack: () -> Unit,
    onItemClick: (String) -> Unit,
    vm: ShoppingListViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    LifecycleEvents { event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            vm.reload()
        }
    }

    ShoppingListScreenContent(
        state = state,
        onBack = onBack,
        onItemClick = onItemClick,
        onAdd = vm::onAdd,
        onRemove = vm::onRemove
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingListScreenContent(
    state: ShoppingListUiState,
    onBack: () -> Unit,
    onItemClick: (String) -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var newItemName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shopping List") },
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
                is ShoppingListUiState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                is ShoppingListUiState.Empty -> {
                    Text(
                        "Your shopping list is empty.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                is ShoppingListUiState.Loaded, is ShoppingListUiState.Error -> {
                    val items = (s as? ShoppingListUiState.Loaded)?.items
                        ?: (s as? ShoppingListUiState.Error)?.items
                        ?: emptyList()

                    if (s is ShoppingListUiState.Error) {
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
                            ShoppingListRow(
                                item = item,
                                onClick = { onItemClick(item.id) },
                                onDelete = { onRemove(item.id) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newItemName,
                    onValueChange = { newItemName = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add item") },
                    singleLine = true
                )
                IconButton(onClick = {
                    onAdd(newItemName)
                    newItemName = ""
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add item")
                }
            }
        }
    }
}

@Composable
private fun ShoppingListRow(
    item: ShoppingListItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove ${item.name}",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

private val previewItems = listOf(
    ShoppingListItem(id = "1", name = "Milk", storageId = "s1", createdAt = 1_700_000_000_000L),
    ShoppingListItem(id = "2", name = "Pasta", storageId = null, createdAt = 1_700_100_000_000L),
    ShoppingListItem(id = "3", name = "Soup cans", storageId = "s2", createdAt = 1_700_200_000_000L)
)

@Preview(showBackground = true)
@Composable
private fun ShoppingListScreenLoadingPreview() {
    HomeShelfTheme {
        ShoppingListScreenContent(
            state = ShoppingListUiState.Loading,
            onBack = {},
            onItemClick = {},
            onAdd = {},
            onRemove = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShoppingListScreenEmptyPreview() {
    HomeShelfTheme {
        ShoppingListScreenContent(
            state = ShoppingListUiState.Empty,
            onBack = {},
            onItemClick = {},
            onAdd = {},
            onRemove = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShoppingListScreenLoadedPreview() {
    HomeShelfTheme {
        ShoppingListScreenContent(
            state = ShoppingListUiState.Loaded(previewItems),
            onBack = {},
            onItemClick = {},
            onAdd = {},
            onRemove = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShoppingListScreenErrorPreview() {
    HomeShelfTheme {
        ShoppingListScreenContent(
            state = ShoppingListUiState.Error(
                message = "Failed to load shopping list",
                items = previewItems
            ),
            onBack = {},
            onItemClick = {},
            onAdd = {},
            onRemove = {}
        )
    }
}
