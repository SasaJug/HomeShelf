package com.jugurdzija.homeshelf.ui.golden

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jugurdzija.homeshelf.data.CaptureData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldenDetailsScreen(
    onBack: () -> Unit,
    vm: GoldenDetailsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
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
                is GoldenDetailsViewModel.State.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is GoldenDetailsViewModel.State.NotFound -> {
                    Text(
                        "Comparison photo not found.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is GoldenDetailsViewModel.State.Loaded -> {
                    GoldenDetailsContent(s.captureData)
                }
            }
        }
    }
}

@Composable
private fun GoldenDetailsContent(captureData: CaptureData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        captureData.bitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
            )
        }

        Spacer(Modifier.height(4.dp))
        Text("Metadata", style = MaterialTheme.typography.titleSmall)

        MetaRow("Name", captureData.name ?: "-")
        MetaRow("Reference", captureData.referenceLabel ?: "-")
        MetaRow("Similarity score", captureData.similarityScore?.let { "%.4f".format(it) } ?: "-")
        MetaRow("Threshold", captureData.similarityThreshold?.let { "%.4f".format(it) } ?: "-")
        MetaRow("Frames analyzed", captureData.framesAnalyzed?.toString() ?: "-")
        MetaRow("Capture attempt", captureData.captureAttempt?.toString() ?: "-")
        captureData.bitmap?.let { MetaRow("Image dimensions", "${it.width} × ${it.height}") }

        captureData.allMatchScores?.takeIf { it.size > 1 }?.let { scores ->
            Spacer(Modifier.height(4.dp))
            Text("All match scores", style = MaterialTheme.typography.titleSmall)
            scores.entries.sortedByDescending { it.value }.forEach { (label, score) ->
                MetaRow(label, "%.4f".format(score))
            }
        }

        if (captureData.groundTruth.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("Ground Truth", style = MaterialTheme.typography.titleSmall)
            captureData.groundTruth.sortedBy { it.cellIndex }.forEach { cell ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Cell ${cell.cellIndex}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .background(cell.changeType.chipColor.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                cell.changeType.symbol,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(cell.changeType.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
