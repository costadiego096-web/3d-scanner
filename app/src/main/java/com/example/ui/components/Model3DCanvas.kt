package com.example.ui.components

import android.graphics.Paint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.Model3D
import com.example.model.Vector3D
import com.example.model.Vertex3D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class ShadingMode {
    TEXTURED,
    SOLID,
    WIREFRAME,
    POINT_CLOUD
}

@Composable
fun Model3DCanvas(
    model: Model3D,
    modifier: Modifier = Modifier,
    shadingMode: ShadingMode = ShadingMode.TEXTURED,
    isAutoRotating: Boolean = false,
    lightDirection: Vector3D = Vector3D(0.5f, 0.8f, 1.0f).normalized(),
    initialPitch: Float = 15f,
    initialYaw: Float = 45f,
    onResetRequested: Boolean = false,
    onResetCompleted: () -> Unit = {}
) {
    var pitch by remember { mutableFloatStateOf(initialPitch) }
    var yaw by remember { mutableFloatStateOf(initialYaw) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    // Reset camera when requested
    LaunchedEffect(onResetRequested) {
        if (onResetRequested) {
            pitch = initialPitch
            yaw = initialYaw
            zoomScale = 1.0f
            panX = 0f
            panY = 0f
            onResetCompleted()
        }
    }

    // Auto-rotation turntable transition
    val infiniteTransition = rememberInfiniteTransition(label = "turntable")
    val turntableYaw by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "turntableYaw"
    )

    val currentYaw = if (isAutoRotating) (yaw + turntableYaw) % 360f else yaw

    val vertices = model.normalizedVertices
    val faces = model.faces

    Box(
        modifier = modifier
            .background(Color(0xFF0D1117))
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    // Zoom
                    zoomScale = (zoomScale * zoom).coerceIn(0.4f, 4.0f)
                    // Pan
                    panX += pan.x
                    panY += pan.y
                }
            }
            .pointerInput(Unit) {
                // Single drag for 3D orbital rotation
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes
                        if (changes.size == 1 && changes[0].pressed) {
                            val change = changes[0]
                            val dragAmount = change.position - change.previousPosition
                            yaw = (yaw + dragAmount.x * 0.45f) % 360f
                            pitch = (pitch - dragAmount.y * 0.45f).coerceIn(-85f, 85f)
                            change.consume()
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f + panX
            val centerY = height / 2f + panY
            val baseScale = min(width, height) * 0.42f * zoomScale

            // Draw technical 3D grid in background
            drawTechnicalGrid(width, height, centerX, centerY, currentYaw, pitch)

            if (vertices.isEmpty()) return@Canvas

            // Compute trig functions for rotation
            val radYaw = (currentYaw * PI / 180f).toFloat()
            val radPitch = (pitch * PI / 180f).toFloat()

            val cosY = cos(radYaw)
            val sinY = sin(radYaw)
            val cosP = cos(radPitch)
            val sinP = sin(radPitch)

            // Perspective camera parameters
            val cameraDist = 3.6f

            // Project all vertices
            val projectedPoints = Array(vertices.size) { i ->
                val v = vertices[i]
                // 1. Rotate around Y (yaw)
                val x1 = v.x * cosY + v.z * sinY
                val y1 = v.y
                val z1 = -v.x * sinY + v.z * cosY

                // 2. Rotate around X (pitch)
                val x2 = x1
                val y2 = y1 * cosP - z1 * sinP
                val z2 = y1 * sinP + z1 * cosP

                // 3. Perspective division
                val zEffective = cameraDist - z2
                val persp = if (zEffective > 0.1f) cameraDist / zEffective else 1.0f

                val screenX = centerX + x2 * baseScale * persp
                val screenY = centerY - y2 * baseScale * persp

                ProjectedVertex(screenX, screenY, z2, v.color)
            }

            when (shadingMode) {
                ShadingMode.POINT_CLOUD -> {
                    // Draw point cloud with depth sizing
                    for (pv in projectedPoints) {
                        val ptAlpha = ((pv.z + 1.2f) / 2.4f).coerceIn(0.25f, 1.0f)
                        val ptRadius = max(1.5f, 3.5f * (pv.z + 1.5f) / 2f)
                        drawCircle(
                            color = Color(pv.color).copy(alpha = ptAlpha),
                            radius = ptRadius,
                            center = Offset(pv.x, pv.y)
                        )
                    }
                }

                ShadingMode.WIREFRAME -> {
                    // Draw wireframe edges
                    val wireColor = Color(0xFF00D2FF)
                    val edgePath = Path()
                    val visitedEdges = HashSet<Long>()

                    for (face in faces) {
                        val p1 = projectedPoints[face.v1]
                        val p2 = projectedPoints[face.v2]
                        val p3 = projectedPoints[face.v3]

                        drawEdge(edgePath, visitedEdges, face.v1, face.v2, p1, p2)
                        drawEdge(edgePath, visitedEdges, face.v2, face.v3, p2, p3)
                        drawEdge(edgePath, visitedEdges, face.v3, face.v1, p3, p1)
                    }

                    drawPath(
                        path = edgePath,
                        color = wireColor.copy(alpha = 0.75f),
                        style = Stroke(width = 1.6f)
                    )
                }

                ShadingMode.SOLID, ShadingMode.TEXTURED -> {
                    // Painter's algorithm: sort faces from back to front by average Z
                    data class SortedFace(
                        val faceIndex: Int,
                        val avgZ: Float,
                        val p1: ProjectedVertex,
                        val p2: ProjectedVertex,
                        val p3: ProjectedVertex
                    )

                    val sortedFaces = ArrayList<SortedFace>(faces.size)
                    for (i in faces.indices) {
                        val f = faces[i]
                        val p1 = projectedPoints[f.v1]
                        val p2 = projectedPoints[f.v2]
                        val p3 = projectedPoints[f.v3]

                        // Backface culling: 2D cross product sign in screen space
                        val cross2D = (p2.x - p1.x) * (p3.y - p1.y) - (p2.y - p1.y) * (p3.x - p1.x)
                        if (cross2D > 0f) {
                            val avgZ = (p1.z + p2.z + p3.z) / 3f
                            sortedFaces.add(SortedFace(i, avgZ, p1, p2, p3))
                        }
                    }

                    // Sort back to front (smallest Z to largest Z)
                    sortedFaces.sortBy { it.avgZ }

                    val facePath = Path()
                    val nativeCanvas = drawContext.canvas.nativeCanvas
                    val paint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.FILL
                    }
                    val strokePaint = Paint().apply {
                        isAntiAlias = true
                        style = Paint.Style.STROKE
                        strokeWidth = 0.75f
                    }

                    val androidPath = android.graphics.Path()

                    for (sf in sortedFaces) {
                        val face = faces[sf.faceIndex]

                        // Transform face normal with same yaw and pitch
                        val n = face.normal
                        val nx1 = n.x * cosY + n.z * sinY
                        val ny1 = n.y
                        val nz1 = -n.x * sinY + n.z * cosY

                        val nx = nx1
                        val ny = ny1 * cosP - nz1 * sinP
                        val nz = ny1 * sinP + nz1 * cosP

                        val transformedNormal = Vector3D(nx, ny, nz).normalized()

                        // Lambertian directional lighting: dot product with light source
                        val nDotL = max(0f, transformedNormal.dot(lightDirection))
                        val ambient = 0.35f
                        val diffuse = 0.65f * nDotL
                        val intensity = (ambient + diffuse).coerceIn(0.15f, 1.0f)

                        val faceColor = if (shadingMode == ShadingMode.TEXTURED) {
                            // Average vertex colors modulated by light intensity
                            val c1 = sf.p1.color
                            val c2 = sf.p2.color
                            val c3 = sf.p3.color

                            val r = (((android.graphics.Color.red(c1) + android.graphics.Color.red(c2) + android.graphics.Color.red(c3)) / 3) * intensity).toInt().coerceIn(0, 255)
                            val g = (((android.graphics.Color.green(c1) + android.graphics.Color.green(c2) + android.graphics.Color.green(c3)) / 3) * intensity).toInt().coerceIn(0, 255)
                            val b = (((android.graphics.Color.blue(c1) + android.graphics.Color.blue(c2) + android.graphics.Color.blue(c3)) / 3) * intensity).toInt().coerceIn(0, 255)
                            android.graphics.Color.rgb(r, g, b)
                        } else {
                            // Solid neutral cyber clay shader
                            val grey = (210 * intensity).toInt().coerceIn(30, 255)
                            android.graphics.Color.rgb(grey, (grey * 0.96f).toInt(), (grey * 1.05f).toInt().coerceAtMost(255))
                        }

                        androidPath.reset()
                        androidPath.moveTo(sf.p1.x, sf.p1.y)
                        androidPath.lineTo(sf.p2.x, sf.p2.y)
                        androidPath.lineTo(sf.p3.x, sf.p3.y)
                        androidPath.close()

                        paint.color = faceColor
                        nativeCanvas.drawPath(androidPath, paint)

                        // Subtle polygon seam highlight
                        strokePaint.color = android.graphics.Color.argb(45, 0, 0, 0)
                        nativeCanvas.drawPath(androidPath, strokePaint)
                    }
                }
            }
        }
    }
}

private class ProjectedVertex(
    val x: Float,
    val y: Float,
    val z: Float,
    val color: Int
)

private fun drawEdge(
    path: Path,
    visited: HashSet<Long>,
    v1: Int,
    v2: Int,
    p1: ProjectedVertex,
    p2: ProjectedVertex
) {
    val minIdx = min(v1, v2)
    val maxIdx = max(v1, v2)
    val edgeKey = (minIdx.toLong() shl 32) or (maxIdx.toLong() and 0xFFFFFFFFL)

    if (visited.add(edgeKey)) {
        path.moveTo(p1.x, p1.y)
        path.lineTo(p2.x, p2.y)
    }
}

private fun DrawScope.drawTechnicalGrid(
    width: Float,
    height: Float,
    centerX: Float,
    centerY: Float,
    yaw: Float,
    pitch: Float
) {
    val gridColor = Color(0xFF1E2633).copy(alpha = 0.5f)
    val axisColor = Color(0xFF00D2FF).copy(alpha = 0.25f)

    // Floor horizon grid
    val floorY = centerY + height * 0.38f
    drawLine(
        color = axisColor,
        start = Offset(0f, floorY),
        end = Offset(width, floorY),
        strokeWidth = 1.5f
    )

    // Perspective floor lines
    for (i in -4..4) {
        val xOffset = i * (width / 9f)
        drawLine(
            color = gridColor,
            start = Offset(centerX + xOffset * 0.4f, centerY + height * 0.15f),
            end = Offset(centerX + xOffset * 1.5f, height),
            strokeWidth = 1f
        )
    }
}
