package com.jugurdzija.homeshelf.ui.shoppinglist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jugurdzija.homeshelf.ui.theme.HomeShelfTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListItemDetailScreen(
    onBack: () -> Unit,
    vm: ShoppingListItemDetailViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    ShoppingListItemDetailScreenContent(
        state = state,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingListItemDetailScreenContent(
    state: ShoppingListItemDetailUiState,
    onBack: () -> Unit
) {
    val title = (state as? ShoppingListItemDetailUiState.Loaded)?.itemName.orEmpty()

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
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (state) {
                is ShoppingListItemDetailUiState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                is ShoppingListItemDetailUiState.Loaded -> {
                    Text(
                        text = state.storageName?.let { "From: $it" } ?: "Added manually",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShoppingListItemDetailScreenPreview() {
    HomeShelfTheme {
        ShoppingListItemDetailScreenContent(
            state = ShoppingListItemDetailUiState.Loaded(itemName = "Soup cans", storageName = "Pantry Shelf A"),
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShoppingListItemDetailScreenManualPreview() {
    HomeShelfTheme {
        ShoppingListItemDetailScreenContent(
            state = ShoppingListItemDetailUiState.Loaded(itemName = "Pasta", storageName = null),
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShoppingListItemDetailScreenLoadingPreview() {
    HomeShelfTheme {
        ShoppingListItemDetailScreenContent(
            state = ShoppingListItemDetailUiState.Loading,
            onBack = {}
        )
    }
}
