package com.jugurdzija.homeshelf.ui.golden

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoldenSaveScreen(
    onBack: () -> Unit,
    vm: GoldenSaveViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val saveState by vm.saveState.collectAsState()
    val holder = GoldenCaptureHolder

    val timestamp = remember {
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    }
    var name by remember {
        mutableStateOf("${holder.referenceLabel ?: "unknown"}_$timestamp")
    }

    LaunchedEffect(saveState) {
        if (saveState is GoldenSaveViewModel.SaveState.Saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Save Golden Photo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Discard")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            holder.bitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(4.dp))
            Text("Metadata", style = MaterialTheme.typography.titleSmall)

            MetaRow("Reference", holder.referenceLabel ?: "-")
            MetaRow("Similarity score", holder.similarityScore?.let { "%.4f".format(it) } ?: "-")
            MetaRow("Threshold", holder.similarityThreshold?.let { "%.4f".format(it) } ?: "-")
            MetaRow("Frames analyzed", holder.framesAnalyzed?.toString() ?: "-")
            MetaRow("Capture attempt", holder.captureAttempt?.toString() ?: "-")

            holder.allMatchScores?.takeIf { it.size > 1 }?.let { scores ->
                Spacer(Modifier.height(4.dp))
                Text("All match scores", style = MaterialTheme.typography.titleSmall)
                scores.entries.sortedByDescending { it.value }.forEach { (label, score) ->
                    MetaRow(label, "%.4f".format(score))
                }
            }

            Spacer(Modifier.height(8.dp))

            when (val s = saveState) {
                is GoldenSaveViewModel.SaveState.Error -> {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                }
                else -> {}
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text("Discard")
                }
                Button(
                    onClick = { vm.save(context, name.trim()) },
                    modifier = Modifier.weight(1f),
                    enabled = saveState !is GoldenSaveViewModel.SaveState.Saving && name.isNotBlank()
                ) {
                    if (saveState is GoldenSaveViewModel.SaveState.Saving) {
                        CircularProgressIndicator()
                    } else {
                        Text("Save")
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
