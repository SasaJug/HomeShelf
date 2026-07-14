package com.jugurdzija.homeshelf.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jugurdzija.homeshelf.data.GuideLine

@Composable
fun DetectionBadge(
    detectedName: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 4.dp
    ) {
        Text(
            text = if (detectedName != null) "Detected: $detectedName" else "New storage",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DetectionBadgeDetectedPreview() {
    DetectionBadge(detectedName = "Pantry Shelf")
}

@Preview(showBackground = true)
@Composable
fun DetectionBadgeNewPreview() {
    DetectionBadge(detectedName = null)
}

@Preview(showBackground = true)
@Composable
fun GuideLineOverlayEmptyPreview() {
    GuideLineOverlay(guideLines = emptyList(), modifier = Modifier.size(300.dp, 400.dp))
}

@Preview(showBackground = true)
@Composable
fun GuideLineOverlayLoadedPreview() {
    GuideLineOverlay(
        modifier = Modifier.size(300.dp, 400.dp),
        guideLines = listOf(
            GuideLine(id = 0, isHorizontal = true, position = 0.33f),
            GuideLine(id = 1, isHorizontal = true, position = 0.66f),
            GuideLine(id = 2, isHorizontal = false, position = 0.5f),
        )
    )
}

@Composable
fun GuideLineOverlay(
    guideLines: List<GuideLine>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        guideLines.forEach { line ->
            if (line.isHorizontal) {
                val y = line.position * size.height
                drawLine(Color.Yellow, Offset(0f, y), Offset(size.width, y), stroke)
            } else {
                val x = line.position * size.width
                drawLine(Color.Yellow, Offset(x, 0f), Offset(x, size.height), stroke)
            }
        }
    }
}
