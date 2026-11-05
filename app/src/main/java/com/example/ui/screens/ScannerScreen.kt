package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.engine.CapturedFrame
import com.example.ui.components.CameraController
import com.example.ui.components.CameraPreviewView
import com.example.ui.components.OrbitalGuidanceOverlay
import com.example.viewmodel.ScannerViewModel

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sensorState by viewModel.sensorState.collectAsState()
    val capturedFrames by viewModel.capturedFrames.collectAsState()
    val isAutoCapture by viewModel.isAutoCaptureEnabled.collectAsState()

    var cameraController by remember { mutableStateOf<CameraController?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Permesso fotocamera necessario per la scansione", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Auto-capture check whenever sensor orientation changes
    LaunchedEffect(sensorState, isAutoCapture) {
        if (isAutoCapture && hasCameraPermission) {
            viewModel.checkAutoCapture {
                cameraController?.takePhoto(
                    onPhotoCaptured = { bitmap ->
                        viewModel.addCapturedFrame(bitmap)
                    },
                    onError = {
                        // In case of error (e.g. preview only or emulator), create a simulated angle frame
                        val dummyBitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
                        viewModel.addCapturedFrame(dummyBitmap)
                    }
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            // Live CameraX Feed
            CameraPreviewView(
                modifier = Modifier.fillMaxSize(),
                onControllerReady = { controller ->
                    cameraController = controller
                }
            )

            // Orbital Guidance 360 HUD Overlay
            OrbitalGuidanceOverlay(
                sensorState = sensorState,
                modifier = Modifier.fillMaxSize()
            )

            // Top Bar Overlay
            TopScannerBar(
                azimuth = sensorState.azimuthDegrees,
                pitch = sensorState.pitchDegrees,
                isAutoCapture = isAutoCapture,
                onToggleAutoCapture = { viewModel.toggleAutoCapture() },
                onBack = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp)
            )

            // Bottom Bar Overlay
            BottomScannerControls(
                capturedFrames = capturedFrames,
                sensorState = sensorState,
                onCaptureClick = {
                    cameraController?.takePhoto(
                        onPhotoCaptured = { bitmap ->
                            viewModel.addCapturedFrame(bitmap)
                        },
                        onError = {
                            val dummyBitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
                            viewModel.addCapturedFrame(dummyBitmap)
                        }
                    ) ?: run {
                        val dummyBitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
                        viewModel.addCapturedFrame(dummyBitmap)
                    }
                },
                onGenerateMesh = {
                    viewModel.processAndReconstruct()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        } else {
            // Camera Permission Needed View
            PermissionRequiredCard(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onBack = onBack,
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
            )
        }
    }
}

@Composable
private fun TopScannerBar(
    azimuth: Float,
    pitch: Float,
    isAutoCapture: Boolean,
    onToggleAutoCapture: () -> Unit,
    onBack: () -> Unit,
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
                .background(Color.Black.copy(alpha = 0.65f))
                .border(1.dp, Color(0xFF30363D), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Indietro",
                tint = Color(0xFFF0F6FC)
            )
        }

        // Telemetry Pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.65f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = Color(0xFF00D2FF),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${azimuth.toInt()}° Azim | ${pitch.toInt()}° P",
                    color = Color(0xFFF0F6FC),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Auto-Capture Toggle Pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isAutoCapture) Color(0xFF004E60).copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.65f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isAutoCapture) Color(0xFF00D2FF) else Color(0xFF30363D)
            ),
            modifier = Modifier.clickable(onClick = onToggleAutoCapture)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (isAutoCapture) Color(0xFF00D2FF) else Color(0xFF8B949E),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isAutoCapture) "Auto" else "Manuale",
                    color = if (isAutoCapture) Color(0xFFB8EAFF) else Color(0xFF8B949E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BottomScannerControls(
    capturedFrames: List<CapturedFrame>,
    sensorState: com.example.sensors.SensorOrientationState,
    onCaptureClick: () -> Unit,
    onGenerateMesh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Thumbnail tray of captured angles (if any)
        if (capturedFrames.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(capturedFrames) { frame ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00D2FF)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        if (frame.bitmap != null && !frame.bitmap.isRecycled) {
                            Image(
                                bitmap = frame.bitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${frame.azimuthDegrees.toInt()}°",
                                    fontSize = 9.sp,
                                    color = Color(0xFF00D2FF),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Sector count progress badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.Black.copy(alpha = 0.75f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
        ) {
            Text(
                text = "${capturedFrames.size} fotogrammi • ${sensorState.capturedSectors.size}/${sensorState.totalSectors} settori 360°",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (sensorState.capturedSectors.size >= 8) Color(0xFF10B981) else Color(0xFF00D2FF),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Actions Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder spacer to center shutter
            Box(modifier = Modifier.size(52.dp))

            // Shutter Button
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .border(2.dp, Color(0xFF00D2FF), CircleShape)
                    .clickable(onClick = onCaptureClick),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00D2FF))
                )
            }

            // Generate Mesh Button
            val canGenerate = capturedFrames.size >= 3
            Button(
                onClick = onGenerateMesh,
                enabled = canGenerate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF21262D),
                    disabledContentColor = Color(0xFF8B949E)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Genera 3D",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PermissionRequiredCard(
    onRequestPermission: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF30363D))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color(0xFF00D2FF),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Accesso Fotocamera Necessario",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF0F6FC)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Per effettuare scansioni 3D fotogrammetriche, Scan3D Studio ha bisogno di accedere alla fotocamera per catturare i diversi angoli dell'oggetto.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF8B949E),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D2FF), contentColor = Color(0xFF002733)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Consenti Accesso Fotocamera", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color(0xFF8B949E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Torna alla Libreria")
            }
        }
    }
}
