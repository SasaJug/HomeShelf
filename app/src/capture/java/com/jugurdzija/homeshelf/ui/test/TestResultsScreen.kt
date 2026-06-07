package com.jugurdzija.homeshelf.ui.test

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestResultsScreen(
    onBack: () -> Unit,
    vm: TestViewModel
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            if (event is TestViewModel.Event.ShareFile) {
                shareFile(context, event.file)
            }
        }
    }

    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Results") },
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
                is TestViewModel.State.Done -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(s.run.results) { result ->
                            TestImageResultCard(result)
                            HorizontalDivider()
                        }
                    }
                    Button(
                        onClick = vm::saveAndShare,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text("Save & Share JSON")
                    }
                }
                else -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
private fun TestImageResultCard(result: TestImageResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(result.goldenName, style = MaterialTheme.typography.titleSmall)
        Text(
            "Reference: ${result.referenceLabel}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!result.alignmentSuccess) {
            Text(
                "Alignment failed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else if (result.cells.isEmpty()) {
            Text(
                "No cells extracted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            CellTableHeader()
            result.cells.forEach { cell ->
                CellTableRow(cell)
            }
        }
    }
}

@Composable
private fun CellTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            "Cell",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(0.15f)
        )
        Text(
            "Similarity",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(0.3f)
        )
        Text(
            "Ground Truth",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(0.55f)
        )
    }
}

@Composable
private fun CellTableRow(cell: TestCellResult) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${cell.cellIndex}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.15f)
        )
        Text(
            "%.4f".format(cell.similarityScore),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.3f)
        )
        Text(
            cell.groundTruth,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.55f)
        )
    }
}

private fun shareFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share test results"))
}
