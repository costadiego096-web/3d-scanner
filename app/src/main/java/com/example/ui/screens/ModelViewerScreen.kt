package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Model3D
import com.example.ui.components.Model3DCanvas
import com.example.ui.components.ObjExportDialog
import com.example.ui.components.ShadingMode
import java.util.Locale

@Composable
fun ModelViewerScreen(
    model: Model3D,
    onBack: () -> Unit
) {
    var shadingMode by remember { mutableStateOf(ShadingMode.TEXTURED) }
    var isAutoRotating by remember { mutableStateOf(false) }
    var triggerReset by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1117))) {
        // Fullscreen Interactive 3D Canvas
        Model3DCanvas(
            model = model,
            modifier = Modifier.fillMaxSize(),
            shadingMode = shadingMode,
            isAutoRotating = isAutoRotating,
            onResetRequested = triggerReset,
            onResetCompleted = { triggerReset = false }
        )

        // Top Bar: Back, Title & Export Action
        TopViewerBar(
            model = model,
            onBack = onBack,
            onExportClick = { showExportDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
        )

        // Floating Shading Modes Bar (Under Top Bar)
        ShadingModeBar(
            currentMode = shadingMode,
            onModeSelected = { shadingMode = it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 104.dp)
        )

        // Floating Camera Tools (Right Side)
        FloatingToolsColumn(
            isAutoRotating = isAutoRotating,
            onToggleTurntable = { isAutoRotating = !isAutoRotating },
            onResetCamera = { triggerReset = true },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )

        // Bottom Info & Export Panel
        BottomViewerPanel(
            model = model,
            onExportClick = { showExportDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    if (showExportDialog) {
        ObjExportDialog(
            model = model,
            onDismissRequest = { showExportDialog = false }
        )
    }
}

@Composable
private fun TopViewerBar(
    model: Model3D,
    onBack: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF161B22).copy(alpha = 0.85f))
                .border(1.dp, Color(0xFF30363D), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Indietro",
                tint = Color(0xFFF0F6FC)
            )
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF161B22).copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF0F6FC),
                    maxLines = 1
                )
                Text(
                    text = "${model.vertexCount} vertici • ${model.faceCount} facce",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = Color(0xFF00D2FF)
                )
            }
        }

        IconButton(
            onClick = onExportClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF00D2FF))
        ) {
            Icon(
                imageVector = Icons.Default.FileUpload,
                contentDescription = "Esporta OBJ",
                tint = Color(0xFF002733)
            )
        }
    }
}

@Composable
private fun ShadingModeBar(
    currentMode: ShadingMode,
    onModeSelected: (ShadingMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF161B22).copy(alpha = 0.88f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ShadingModePill(
                title = "Texture",
                icon = Icons.Default.Palette,
                isSelected = currentMode == ShadingMode.TEXTURED,
                onClick = { onModeSelected(ShadingMode.TEXTURED) }
            )
            ShadingModePill(
                title = "Solido",
                icon = Icons.Default.Layers,
                isSelected = currentMode == ShadingMode.SOLID,
                onClick = { onModeSelected(ShadingMode.SOLID) }
            )
            ShadingModePill(
                title = "Wireframe",
                icon = Icons.Default.Grid4x4,
                isSelected = currentMode == ShadingMode.WIREFRAME,
                onClick = { onModeSelected(ShadingMode.WIREFRAME) }
            )
            ShadingModePill(
                title = "Punti",
                icon = Icons.Default.Grain,
                isSelected = currentMode == ShadingMode.POINT_CLOUD,
                onClick = { onModeSelected(ShadingMode.POINT_CLOUD) }
            )
        }
    }
}

@Composable
private fun ShadingModePill(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF00D2FF) else Color.Transparent,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF002733) else Color(0xFF8B949E),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF002733) else Color(0xFF8B949E)
            )
        }
    }
}

@Composable
private fun FloatingToolsColumn(
    isAutoRotating: Boolean,
    onToggleTurntable: () -> Unit,
    onResetCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Turntable Auto-Rotate Toggle
        IconButton(
            onClick = onToggleTurntable,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isAutoRotating) Color(0xFF004E60) else Color(0xFF161B22).copy(alpha = 0.85f))
                .border(1.dp, if (isAutoRotating) Color(0xFF00D2FF) else Color(0xFF30363D), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = "Auto-Rotazione Turntable",
                tint = if (isAutoRotating) Color(0xFF00D2FF) else Color(0xFF8B949E),
                modifier = Modifier.size(20.dp)
            )
        }

        // Reset View Button
        IconButton(
            onClick = onResetCamera,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF161B22).copy(alpha = 0.85f))
                .border(1.dp, Color(0xFF30363D), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = "Reset Vista",
                tint = Color(0xFF8B949E),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun BottomViewerPanel(
    model: Model3D,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bb = model.boundingBox
    val sizeX = String.format(Locale.US, "%.1f", bb.width * 10f)
    val sizeY = String.format(Locale.US, "%.1f", bb.height * 10f)
    val sizeZ = String.format(Locale.US, "%.1f", bb.depth * 10f)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22).copy(alpha = 0.92f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dimensioni Bounding Box",
                        fontSize = 11.sp,
                        color = Color(0xFF8B949E)
                    )
                    Text(
                        text = "$sizeX × $sizeY × $sizeZ cm",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF0F6FC)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF00D2FF).copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00D2FF).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "${model.vertexCount} vertici",
                            fontSize = 11.sp,
                            color = Color(0xFF00D2FF),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "${model.faceCount} facce",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Action Button: Esporta OBJ
            Button(
                onClick = onExportClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00D2FF),
                    contentColor = Color(0xFF002733)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileUpload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Esporta Modello OBJ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
