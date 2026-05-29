package com.jugurdzija.homeshelf.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jugurdzija.homeshelf.data.GuideLine
import com.jugurdzija.homeshelf.data.ReferenceItem
import com.jugurdzija.homeshelf.embedding.ReferenceMatch
import java.io.File

@Composable
fun MatchesOverlay(
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

@Preview(showBackground = true)
@Composable
fun MatchesOverlayEmptyPreview() {
    MatchesOverlay(matches = emptyList())
}

@Preview(showBackground = true)
@Composable
fun MatchesOverlayLoadedPreview() {
    val fakeFile = File("/fake/path.jpg")
    MatchesOverlay(
        matches = listOf(
            ReferenceMatch(ReferenceItem("ref_1", "Reference 1", fakeFile), similarity = 0.94, inferenceMs = 12),
            ReferenceMatch(ReferenceItem("ref_2", "Reference 2", fakeFile), similarity = 0.72, inferenceMs = 11),
            ReferenceMatch(ReferenceItem("ref_3", "Reference 3", fakeFile), similarity = 0.45, inferenceMs = 10),
        )
    )
}

@Composable
fun similarityColor(similarity: Double) = when {
    similarity >= 0.9 -> MaterialTheme.colorScheme.primary
    similarity >= 0.7 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.error
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
