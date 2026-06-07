package com.jugurdzija.homeshelf.ui.test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestRunScreen(
    onNavigateToResults: () -> Unit,
    vm: TestViewModel
) {
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            if (event is TestViewModel.Event.NavigateToResults) {
                onNavigateToResults()
            }
        }
    }

    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Running Tests") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(24.dp))
            when (val s = state) {
                is TestViewModel.State.Running -> {
                    Text(
                        "${s.current} of ${s.total} images processed",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> {
                    Text("Processing…", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
