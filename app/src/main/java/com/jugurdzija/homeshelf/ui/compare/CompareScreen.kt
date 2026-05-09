package com.jugurdzija.homeshelf.ui.compare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.jugurdzija.homeshelf.embedding.ReferenceMatch
import com.jugurdzija.homeshelf.ui.common.FrameThrottlingAnalyzer
import com.jugurdzija.homeshelf.ui.common.GUIDE_IMAGE_TRANSPARENCY
import com.jugurdzija.homeshelf.ui.common.bindCameraX
import com.jugurdzija.homeshelf.ui.detail.BitmapDetailHolder
import org.opencv.android.OpenCVLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    onBack: () -> Unit,
    onNavigateToDetail: () -> Unit,
) {

    val vm: CompareViewModel = hiltViewModel()
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        OpenCVLoader.initLocal()
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) vm.onPermissionDenied()
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compare") },
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
                is CompareUiState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                is CompareUiState.MissingReference -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = s.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onBack) { Text("Go Back") }
                    }
                }

                is CompareUiState.PermissionDenied -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Camera permission is required to compare images.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            )
                        }) { Text("Open Settings") }
                    }
                }

                is CompareUiState.Streaming, is CompareUiState.Error, is CompareUiState.Captured -> {
                    if (hasPermission) {
                        val previewView = remember { PreviewView(context) }

                        LaunchedEffect(previewView) {
                            bindCameraX(
                                context = context,
                                lifecycleOwner = lifecycleOwner,
                                previewView = previewView,
                                analyzer = FrameThrottlingAnalyzer(skipFactor = 15) { bitmap ->
                                    vm.onFrameReceived(bitmap)
                                }
                            )
                        }

                        LaunchedEffect(s is CompareUiState.Captured) {
                            if (s is CompareUiState.Captured) {
                                vm.onCapturedFrame(s.frameBitmap)
                            }
                        }

                        if (s is CompareUiState.Captured) {
                            Image(
                                bitmap = s.frameBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Aligning…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        } else {
                            AndroidView(
                                factory = { previewView },
                                modifier = Modifier.fillMaxSize()
                            )

                            val guideBitmap = (s as? CompareUiState.Streaming)?.guideBitmap
                                ?: (s as? CompareUiState.Error)?.guideBitmap
                            if (guideBitmap != null) {
                                Image(
                                    bitmap = guideBitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                    alpha = GUIDE_IMAGE_TRANSPARENCY
                                )
                            }
                        }

                        val matches = (s as? CompareUiState.Streaming)?.matches
                            ?: (s as? CompareUiState.Error)?.matches
                            ?: (s as? CompareUiState.Captured)?.matches
                            ?: emptyList()

                        MatchesOverlay(
                            matches = matches,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(16.dp)
                        )

                        if (s is CompareUiState.Error) {
                            Text(
                                text = s.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                }

                is CompareUiState.Aligned -> {
                    Image(
                        bitmap = s.alignedBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    MatchesOverlay(
                        matches = s.matches,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 148.dp)
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                BitmapDetailHolder.pending = s.alignedBitmap
                                onNavigateToDetail()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View Result")
                        }
                        Button(
                            onClick = { vm.onScanAgain() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Scan Again")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchesOverlay(
    matches: List<ReferenceMatch>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (matches.isEmpty()) {
                Text(
                    text = "Scanning…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                matches.forEachIndexed { index, match ->
                    if (index > 0) Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = match.item.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${"%.1f".format(match.similarity * 100)}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                            color = similarityColor(match.similarity)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun similarityColor(similarity: Double) = when {
    similarity >= 0.9 -> MaterialTheme.colorScheme.primary
    similarity >= 0.7 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
}
