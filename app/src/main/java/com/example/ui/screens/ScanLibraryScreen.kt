package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Model3D
import com.example.ui.components.Model3DCanvas
import com.example.ui.components.ObjExportDialog
import com.example.ui.components.ShadingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScanLibraryScreen(
    models: List<Model3D>,
    onStartScan: () -> Unit,
    onSelectModel: (Model3D) -> Unit,
    onDeleteModel: (String) -> Unit
) {
    var modelToExport by remember { mutableStateOf<Model3D?>(null) }
    var modelToDelete by remember { mutableStateOf<Model3D?>(null) }

    Scaffold(
        containerColor = Color(0xFF0D1117),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartScan,
                containerColor = Color(0xFF00D2FF),
                contentColor = Color(0xFF002733),
                icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                text = { Text("Nuova Scansione 3D", fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Hero Banner
            item {
                HeroBanner(
                    modelsCount = models.size,
                    totalFaces = models.sumOf { it.faceCount },
                    onStartScan = onStartScan
                )
            }

            // Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Modelli Salvati (${models.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF0F6FC)
                    )
                    Text(
                        text = "Esportabili in .OBJ",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF00D2FF)
                    )
                }
            }

            if (models.isEmpty()) {
                item {
                    EmptyScansPlaceholder(onStartScan = onStartScan)
                }
            } else {
                items(models, key = { it.id }) { model ->
                    ModelScanCard(
                        model = model,
                        onClick = { onSelectModel(model) },
                        onExport = { modelToExport = model },
                        onDelete = { modelToDelete = model }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // OBJ Export Dialog
    modelToExport?.let { model ->
        ObjExportDialog(
            model = model,
            onDismissRequest = { modelToExport = null }
        )
    }

    // Delete Confirmation Dialog
    val targetModel = modelToDelete
    if (targetModel != null) {
        AlertDialog(
            onDismissRequest = { modelToDelete = null },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteModel(targetModel.id)
                        modelToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { modelToDelete = null }) {
                    Text("Annulla", color = Color(0xFF8B949E))
                }
            },
            title = { Text("Eliminare la scansione?") },
            text = { Text("Vuoi davvero rimuovere \"${targetModel.name}\"? Questa operazione non può essere annullata.") },
            containerColor = Color(0xFF161B22),
            titleContentColor = Color(0xFFF0F6FC),
            textContentColor = Color(0xFF8B949E)
        )
    }
}

@Composable
private fun HeroBanner(
    modelsCount: Int,
    totalFaces: Int,
    onStartScan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00D2FF).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF00D2FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = null,
                            tint = Color(0xFF00D2FF),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Scan3D Studio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF0F6FC)
                        )
                        Text(
                            text = "Fotogrammetria & Export Wavefront OBJ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8B949E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0D1117))
                    .border(1.dp, Color(0xFF21262D), RoundedCornerShape(12.dp))
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatColumn(label = "Modelli", value = "$modelsCount", color = Color(0xFF00D2FF))
                Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color(0xFF30363D)))
                StatColumn(label = "Facce 3D", value = "$totalFaces", color = Color(0xFF10B981))
                Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color(0xFF30363D)))
                StatColumn(label = "Formato", value = ".OBJ", color = Color(0xFF8B5CF6))
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 11.sp, color = Color(0xFF8B949E))
    }
}

@Composable
private fun ModelScanCard(
    model: Model3D,
    onClick: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(model.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Column {
            // Interactive 3D Mini-Viewer Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFF0D1117))
            ) {
                Model3DCanvas(
                    model = model,
                    modifier = Modifier.fillMaxSize(),
                    shadingMode = ShadingMode.TEXTURED,
                    isAutoRotating = true,
                    initialPitch = 20f,
                    initialYaw = 35f
                )

                // Category pill tag
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF161B22).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D)),
                    modifier = Modifier.padding(12.dp).align(Alignment.TopStart)
                ) {
                    Text(
                        text = model.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF00D2FF),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Interactive hint
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.padding(12.dp).align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = "Tocca per aprire",
                        fontSize = 10.sp,
                        color = Color(0xFF8B949E),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Card Body Info & Actions
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF0F6FC)
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Elimina",
                            tint = Color(0xFF8B949E).copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8B949E)
                )

                if (model.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = model.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC9D1D9),
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats and Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${model.vertexCount} vertici",
                            fontSize = 11.sp,
                            color = Color(0xFF00D2FF),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = "•", fontSize = 11.sp, color = Color(0xFF30363D))
                        Text(
                            text = "${model.faceCount} facce",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onExport,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00D2FF).copy(alpha = 0.18f),
                            contentColor = Color(0xFF00D2FF)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Esporta OBJ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyScansPlaceholder(onStartScan: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Column(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Sensors,
                contentDescription = null,
                tint = Color(0xFF00D2FF),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nessuna scansione presente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF0F6FC)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Inquadra un oggetto con la fotocamera e ruotagli attorno per generare il tuo primo modello 3D in formato OBJ.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8B949E),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onStartScan,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D2FF), contentColor = Color(0xFF002733)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Avvia Scansione")
            }
        }
    }
}
