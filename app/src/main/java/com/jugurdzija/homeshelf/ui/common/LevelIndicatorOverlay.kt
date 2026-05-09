package com.jugurdzija.homeshelf.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private val GREEN = Color(0xFF4CAF50)
private val RED = Color(0xFFF44336)
private const val TRACK_RANGE = 30f

@Composable
fun LevelIndicatorOverlay(
    orientation: DeviceOrientation,
    modifier: Modifier = Modifier
) {
    val isLevel = abs(orientation.pitch) < LEVEL_THRESHOLD_DEG &&
            abs(orientation.roll) < LEVEL_THRESHOLD_DEG
    val statusColor = if (isLevel) GREEN else RED

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    text = if (isLevel) "Level ready to capture" else "Tilt device to level",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            LevelBar(label = "Pitch", value = orientation.pitch)
            LevelBar(label = "Roll", value = orientation.roll)
        }
    }
}

@Composable
private fun LevelBar(label: String, value: Float) {
    val isOk = abs(value) < LEVEL_THRESHOLD_DEG
    val indicatorColor = if (isOk) GREEN else RED

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(30.dp)
        )
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
        ) {
            val w = size.width
            val h = size.height
            val trackH = h * 0.45f
            val trackY = (h - trackH) / 2f
            val clamped = value.coerceIn(-TRACK_RANGE, TRACK_RANGE)
            val fraction = (clamped + TRACK_RANGE) / (TRACK_RANGE * 2f)

            // Track background
            drawRoundRect(
                color = Color.White.copy(alpha = 0.2f),
                topLeft = Offset(0f, trackY),
                size = Size(w, trackH),
                cornerRadius = CornerRadius(trackH / 2f)
            )

            // Threshold (green zone)
            val thresholdW = w * (LEVEL_THRESHOLD_DEG / TRACK_RANGE)
            drawRoundRect(
                color = GREEN.copy(alpha = 0.35f),
                topLeft = Offset((w - thresholdW) / 2f, trackY),
                size = Size(thresholdW, trackH),
                cornerRadius = CornerRadius(trackH / 2f)
            )

            // Center line
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = Offset(w / 2f, 0f),
                end = Offset(w / 2f, h),
                strokeWidth = 1.5f
            )

            // Indicator dot
            val dotRadius = h / 2f
            val dotX = (w * fraction).coerceIn(dotRadius, w - dotRadius)
            drawCircle(
                color = indicatorColor,
                radius = dotRadius,
                center = Offset(dotX, h / 2f)
            )
        }
        Text(
            text = "%.1f".format(value),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(40.dp)
        )
    }
}
