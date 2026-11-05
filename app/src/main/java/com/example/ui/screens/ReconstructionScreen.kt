package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.ScannerViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ReconstructionScreen(
    viewModel: ScannerViewModel
) {
    val state by viewModel.reconstructionState.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pointCloudSpin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Cyber Point Cloud Synthesizer Visual
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF161B22))
                    .border(2.dp, Color(0xFF00D2FF).copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    val cX = size.width / 2f
                    val cY = size.height / 2f
                    val maxRadius = size.width * 0.40f * pulseScale

                    // Synthesized point cloud particles
                    val numPoints = 64
                    for (i in 0 until numPoints) {
                        val angle = (i.toFloat() / numPoints) * 360f + rotationAngle
                        val rad = (angle * PI / 180f).toFloat()
                        val r = (maxRadius * (0.3f + 0.7f * ((i * 17) % 100) / 100f))

                        val px = cX + r * cos(rad)
                        val py = cY + r * sin(rad) * 0.75f // Oblique 3D angle

                        val alpha = (((i * 23) % 80) / 100f + 0.2f).coerceIn(0.2f, 1f)
                        drawCircle(
                            color = Color(0xFF00D2FF).copy(alpha = alpha),
                            radius = 2.5f,
                            center = Offset(px, py)
                        )
                    }

                    // Rotating orbit rings
                    drawCircle(
                        color = Color(0xFF10B981).copy(alpha = 0.35f),
                        radius = maxRadius * 0.8f,
                        center = Offset(cX, cY),
                        style = Stroke(width = 1.5f)
                    )
                }

                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = Color(0xFF00D2FF),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Ricostruzione Mesh 3D",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF0F6FC)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = state.statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF00D2FF),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Smooth Progress Bar
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF00D2FF),
                trackColor = Color(0xFF21262D),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 4 Stages Pipeline Tracker
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StageItem(index = 1, title = "Analisi & Corrispondenza Fotogrammi", currentStage = state.stage)
                    StageItem(index = 2, title = "Generazione Nuvola di Punti (Point Cloud)", currentStage = state.stage)
                    StageItem(index = 3, title = "Triangolazione Facce & Normali", currentStage = state.stage)
                    StageItem(index = 4, title = "Generazione Formato Wavefront OBJ", currentStage = state.stage)
                }
            }
        }
    }
}

@Composable
private fun StageItem(index: Int, title: String, currentStage: Int) {
    val isDone = currentStage > index
    val isCurrent = currentStage == index

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val iconColor = when {
            isDone -> Color(0xFF10B981)
            isCurrent -> Color(0xFF00D2FF)
            else -> Color(0xFF8B949E).copy(alpha = 0.4f)
        }

        Icon(
            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(18.dp)
        )

        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isDone || isCurrent) Color(0xFFF0F6FC) else Color(0xFF8B949E).copy(alpha = 0.6f)
        )
    }
}
