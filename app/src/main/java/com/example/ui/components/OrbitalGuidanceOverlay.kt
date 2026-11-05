package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.sensors.SensorOrientationState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun OrbitalGuidanceOverlay(
    sensorState: SensorOrientationState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanline")
    val scanlineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanlineProgress"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f

            // 1. Central 3D Bounding Target Box
            val boxWidth = width * 0.72f
            val boxHeight = height * 0.44f
            val boxLeft = centerX - boxWidth / 2f
            val boxTop = centerY - boxHeight / 2f

            val cornerLength = 32f
            val reticleColor = Color(0xFF00D2FF).copy(alpha = 0.85f)
            val strokeWidth = 3f

            // Corner brackets
            // Top-Left
            drawLine(reticleColor, Offset(boxLeft, boxTop), Offset(boxLeft + cornerLength, boxTop), strokeWidth)
            drawLine(reticleColor, Offset(boxLeft, boxTop), Offset(boxLeft, boxTop + cornerLength), strokeWidth)

            // Top-Right
            drawLine(reticleColor, Offset(boxLeft + boxWidth, boxTop), Offset(boxLeft + boxWidth - cornerLength, boxTop), strokeWidth)
            drawLine(reticleColor, Offset(boxLeft + boxWidth, boxTop), Offset(boxLeft + boxWidth, boxTop + cornerLength), strokeWidth)

            // Bottom-Left
            drawLine(reticleColor, Offset(boxLeft, boxTop + boxHeight), Offset(boxLeft + cornerLength, boxTop + boxHeight), strokeWidth)
            drawLine(reticleColor, Offset(boxLeft, boxTop + boxHeight), Offset(boxLeft, boxTop + boxHeight - cornerLength), strokeWidth)

            // Bottom-Right
            drawLine(reticleColor, Offset(boxLeft + boxWidth, boxTop + boxHeight), Offset(boxLeft + boxWidth - cornerLength, boxTop + boxHeight), strokeWidth)
            drawLine(reticleColor, Offset(boxLeft + boxWidth, boxTop + boxHeight), Offset(boxLeft + boxWidth, boxTop + boxHeight - cornerLength), strokeWidth)

            // Subtle dashed bounding border
            drawRoundRect(
                color = reticleColor.copy(alpha = 0.25f),
                topLeft = Offset(boxLeft, boxTop),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(
                    width = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            )

            // Sweeping laser scanline
            val laserY = boxTop + boxHeight * scanlineY
            drawLine(
                color = Color(0xFF00E5FF).copy(alpha = 0.75f),
                start = Offset(boxLeft + 8f, laserY),
                end = Offset(boxLeft + boxWidth - 8f, laserY),
                strokeWidth = 2.5f
            )

            // 2. Artificial Horizon / Level Line
            val horizonLength = 80f
            val rollRad = (sensorState.rollDegrees * PI / 180f).toFloat()
            val dx = (horizonLength / 2f) * cos(rollRad)
            val dy = (horizonLength / 2f) * sin(rollRad)
            val levelPitchOffset = (sensorState.pitchDegrees * 1.5f).coerceIn(-40f, 40f)

            val horizonCenter = Offset(centerX, centerY + levelPitchOffset)
            val isLevel = kotlin.math.abs(sensorState.rollDegrees) < 3.0f
            val horizonColor = if (isLevel) Color(0xFF10B981) else Color(0xFFF59E0B)

            drawLine(
                color = horizonColor.copy(alpha = 0.85f),
                start = Offset(horizonCenter.x - dx, horizonCenter.y - dy),
                end = Offset(horizonCenter.x + dx, horizonCenter.y + dy),
                strokeWidth = 2.5f
            )
            drawCircle(
                color = horizonColor,
                radius = 3.5f,
                center = horizonCenter
            )

            // 3. Circular 360° Orbital Ring with 16 Angular Sectors
            val ringRadius = min(width, height) * 0.44f
            val ringCenter = Offset(centerX, centerY)

            // Outer dim orbit track
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = ringRadius,
                center = ringCenter,
                style = Stroke(width = 2f)
            )

            val numSectors = sensorState.totalSectors
            for (i in 0 until numSectors) {
                val sectorAngleDeg = (i.toFloat() / numSectors) * 360f - 90f // Start from top
                val sectorAngleRad = (sectorAngleDeg * PI / 180f).toFloat()

                val isCaptured = sensorState.capturedSectors.contains(i)
                val isCurrent = sensorState.currentSector == i

                val nodeRadius = when {
                    isCurrent -> 9f
                    isCaptured -> 6.5f
                    else -> 4f
                }

                val nodeColor = when {
                    isCurrent && isCaptured -> Color(0xFF10B981)
                    isCurrent -> Color(0xFF00D2FF)
                    isCaptured -> Color(0xFF10B981)
                    else -> Color.White.copy(alpha = 0.35f)
                }

                val nodeX = ringCenter.x + ringRadius * cos(sectorAngleRad)
                val nodeY = ringCenter.y + ringRadius * sin(sectorAngleRad)

                drawCircle(
                    color = nodeColor,
                    radius = nodeRadius,
                    center = Offset(nodeX, nodeY)
                )

                // Current heading pointer beacon
                if (isCurrent) {
                    drawCircle(
                        color = Color(0xFF00D2FF).copy(alpha = 0.4f),
                        radius = 16f,
                        center = Offset(nodeX, nodeY),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }
    }
}
