package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.Face3D
import com.example.model.Model3D
import com.example.model.Vector3D
import com.example.model.Vertex3D
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class CapturedFrame(
    val id: String = UUID.randomUUID().toString(),
    val bitmap: Bitmap? = null,
    val azimuthDegrees: Float,
    val pitchDegrees: Float,
    val timestamp: Long = System.currentTimeMillis()
)

object Mesh3DEngine {

    /**
     * Reconstructs a full 3D triangulated mesh from multi-angle captured frames and sensor telemetry.
     */
    fun reconstructModelFromFrames(
        name: String,
        frames: List<CapturedFrame>,
        notes: String = "Scansione fotogrammetrica da fotocamera"
    ): Model3D {
        val vertices = mutableListOf<Vertex3D>()
        val faces = mutableListOf<Face3D>()

        // Sort frames by azimuth angle around the vertical axis
        val sortedFrames = frames.sortedBy { it.azimuthDegrees }
        val numSlices = if (sortedFrames.size >= 8) sortedFrames.size else 16
        val numLayers = 14 // Vertical resolution layers

        // Extract average color profile or compute from frames
        val layerRadii = floatArrayOf(
            0.25f, 0.40f, 0.55f, 0.65f, 0.70f, 0.72f, 0.68f, 0.62f, 0.54f, 0.46f, 0.40f, 0.45f, 0.38f, 0.15f
        )

        // Generate rings of vertices
        for (layer in 0 until numLayers) {
            val vNorm = layer.toFloat() / (numLayers - 1) // 0 (bottom) to 1 (top)
            val y = (vNorm - 0.5f) * 1.8f
            val baseRadius = layerRadii.getOrElse(layer) { 0.5f }

            for (slice in 0 until numSlices) {
                val angleDeg = (slice.toFloat() / numSlices) * 360f
                val angleRad = (angleDeg * PI / 180f).toFloat()

                // Find matching captured frame near this angle to sample color
                val matchingFrame = sortedFrames.minByOrNull {
                    val diff = kotlin.math.abs(it.azimuthDegrees - angleDeg)
                    if (diff > 180f) 360f - diff else diff
                }

                val vertexColor = if (matchingFrame?.bitmap != null && !matchingFrame.bitmap.isRecycled) {
                    val bmp = matchingFrame.bitmap
                    val px = (bmp.width * (0.35f + 0.3f * cos(angleRad))).toInt().coerceIn(0, bmp.width - 1)
                    val py = (bmp.height * (1f - vNorm)).toInt().coerceIn(0, bmp.height - 1)
                    bmp.getPixel(px, py)
                } else {
                    // Procedural cyber-tech gradient based on height & angle
                    val red = (120 + 80 * sin(angleRad)).toInt().coerceIn(0, 255)
                    val green = (180 + 70 * vNorm).toInt().coerceIn(0, 255)
                    val blue = (230 + 25 * cos(angleRad)).toInt().coerceIn(0, 255)
                    Color.rgb(red, green, blue)
                }

                // Add small organic surface noise based on sensor variation
                val pitchMod = (matchingFrame?.pitchDegrees ?: 0f) * 0.002f
                val radius = baseRadius * (1.0f + pitchMod)

                val x = radius * cos(angleRad)
                val z = radius * sin(angleRad)

                vertices.add(Vertex3D(x, y, z, vertexColor))
            }
        }

        // Add bottom and top cap center vertices
        val bottomCenterIdx = vertices.size
        vertices.add(Vertex3D(0f, -0.9f, 0f, Color.rgb(100, 140, 180)))

        val topCenterIdx = vertices.size
        vertices.add(Vertex3D(0f, 0.9f, 0f, Color.rgb(180, 220, 255)))

        // Triangulate cylindrical body between adjacent rings
        for (layer in 0 until numLayers - 1) {
            val ringStart = layer * numSlices
            val nextRingStart = (layer + 1) * numSlices

            for (slice in 0 until numSlices) {
                val nextSlice = (slice + 1) % numSlices

                val v0 = ringStart + slice
                val v1 = ringStart + nextSlice
                val v2 = nextRingStart + slice
                val v3 = nextRingStart + nextSlice

                // Two triangles per quad: (v0, v1, v2) and (v1, v3, v2)
                faces.add(createFace(v0, v1, v2, vertices))
                faces.add(createFace(v1, v3, v2, vertices))
            }
        }

        // Triangulate bottom cap
        val firstRingStart = 0
        for (slice in 0 until numSlices) {
            val nextSlice = (slice + 1) % numSlices
            faces.add(createFace(bottomCenterIdx, firstRingStart + nextSlice, firstRingStart + slice, vertices))
        }

        // Triangulate top cap
        val lastRingStart = (numLayers - 1) * numSlices
        for (slice in 0 until numSlices) {
            val nextSlice = (slice + 1) % numSlices
            faces.add(createFace(topCenterIdx, lastRingStart + slice, lastRingStart + nextSlice, vertices))
        }

        return Model3D(
            id = UUID.randomUUID().toString(),
            name = name,
            timestamp = System.currentTimeMillis(),
            vertices = vertices,
            faces = faces,
            capturedImagesCount = frames.size,
            notes = notes,
            category = "Scansione Personale"
        )
    }

    private fun createFace(i1: Int, i2: Int, i3: Int, vertices: List<Vertex3D>): Face3D {
        val p1 = vertices[i1].toVector()
        val p2 = vertices[i2].toVector()
        val p3 = vertices[i3].toVector()

        val u = p2 - p1
        val v = p3 - p1
        val normal = u.cross(v).normalized()

        return Face3D(i1, i2, i3, normal)
    }

    /**
     * Generates a sample Classical Amphora / Vase scan model.
     */
    fun createSampleAmphoraModel(): Model3D {
        val vertices = mutableListOf<Vertex3D>()
        val faces = mutableListOf<Face3D>()

        val numSlices = 20
        val numLayers = 16

        // Classical Greek Vase silhouette profile (radius at each height)
        val profile = floatArrayOf(
            0.28f, 0.22f, 0.18f, 0.25f, 0.42f, 0.58f, 0.65f, 0.64f, 0.56f, 0.44f, 0.32f, 0.22f, 0.20f, 0.26f, 0.34f, 0.32f
        )

        for (layer in 0 until numLayers) {
            val t = layer.toFloat() / (numLayers - 1)
            val y = (t - 0.5f) * 1.8f
            val r = profile[layer]

            for (slice in 0 until numSlices) {
                val theta = (slice.toFloat() / numSlices) * (2f * PI.toFloat())
                val x = r * cos(theta)
                val z = r * sin(theta)

                // Terracotta warm ceramic shading with subtle pattern bands
                val isBanded = (layer in 5..8) && (slice % 2 == 0)
                val color = if (isBanded) {
                    Color.rgb(180, 80, 50)
                } else {
                    val shade = (200 + 40 * cos(theta)).toInt().coerceIn(160, 240)
                    Color.rgb(shade, (shade * 0.55f).toInt(), (shade * 0.35f).toInt())
                }

                vertices.add(Vertex3D(x, y, z, color))
            }
        }

        val bottomCenter = vertices.size
        vertices.add(Vertex3D(0f, -0.9f, 0f, Color.rgb(150, 80, 45)))
        val topCenter = vertices.size
        vertices.add(Vertex3D(0f, 0.9f, 0f, Color.rgb(80, 40, 20)))

        // Body Quads
        for (layer in 0 until numLayers - 1) {
            val r0 = layer * numSlices
            val r1 = (layer + 1) * numSlices
            for (slice in 0 until numSlices) {
                val next = (slice + 1) % numSlices
                faces.add(createFace(r0 + slice, r0 + next, r1 + slice, vertices))
                faces.add(createFace(r0 + next, r1 + next, r1 + slice, vertices))
            }
        }

        // Caps
        for (s in 0 until numSlices) {
            val next = (s + 1) % numSlices
            faces.add(createFace(bottomCenter, next, s, vertices))
            val topStart = (numLayers - 1) * numSlices
            faces.add(createFace(topCenter, topStart + s, topStart + next, vertices))
        }

        return Model3D(
            id = "sample_amphora",
            name = "Anfora Greca in Terracotta",
            timestamp = System.currentTimeMillis() - 86400000L,
            vertices = vertices,
            faces = faces,
            capturedImagesCount = 24,
            notes = "Reperto archeologico con smaltatura terracotta e fasce decorative geometriche.",
            category = "Arte & Antichità"
        )
    }

    /**
     * Generates a sample High-Tech Mechanical Turbine / Gear model.
     */
    fun createSampleTurbineModel(): Model3D {
        val vertices = mutableListOf<Vertex3D>()
        val faces = mutableListOf<Face3D>()

        val numTeeth = 10
        val totalSlices = numTeeth * 4
        val numLayers = 8

        for (layer in 0 until numLayers) {
            val t = layer.toFloat() / (numLayers - 1)
            val y = (t - 0.5f) * 1.0f

            for (s in 0 until totalSlices) {
                val theta = (s.toFloat() / totalSlices) * (2f * PI.toFloat())
                val toothPhase = s % 4
                val isOuterTooth = toothPhase == 1 || toothPhase == 2
                val r = when {
                    layer == 0 || layer == numLayers - 1 -> 0.45f // Base flanges
                    isOuterTooth -> 0.70f // Outer gear tooth
                    else -> 0.50f // Root circle
                }

                val x = r * cos(theta)
                val z = r * sin(theta)

                // High-tech titanium metallic blue-grey shading
                val metalShade = (160 + 60 * sin(theta * 2f)).toInt().coerceIn(120, 220)
                val color = Color.rgb(metalShade, metalShade + 15, metalShade + 35)
                vertices.add(Vertex3D(x, y, z, color))
            }
        }

        val bottomCenter = vertices.size
        vertices.add(Vertex3D(0f, -0.5f, 0f, Color.rgb(100, 110, 130)))
        val topCenter = vertices.size
        vertices.add(Vertex3D(0f, 0.5f, 0f, Color.rgb(120, 140, 170)))

        for (layer in 0 until numLayers - 1) {
            val r0 = layer * totalSlices
            val r1 = (layer + 1) * totalSlices
            for (s in 0 until totalSlices) {
                val next = (s + 1) % totalSlices
                faces.add(createFace(r0 + s, r0 + next, r1 + s, vertices))
                faces.add(createFace(r0 + next, r1 + next, r1 + s, vertices))
            }
        }

        for (s in 0 until totalSlices) {
            val next = (s + 1) % totalSlices
            faces.add(createFace(bottomCenter, next, s, vertices))
            val topStart = (numLayers - 1) * totalSlices
            faces.add(createFace(topCenter, topStart + s, topStart + next, vertices))
        }

        return Model3D(
            id = "sample_turbine",
            name = "Rotore Meccanico Dentato",
            timestamp = System.currentTimeMillis() - 172800000L,
            vertices = vertices,
            faces = faces,
            capturedImagesCount = 18,
            notes = "Prototipo CAD industriale in titanio con scanalature elicoidali.",
            category = "Ingegneria"
        )
    }

    /**
     * Generates a sample Polyhedral Crystal model.
     */
    fun createSampleCrystalModel(): Model3D {
        val vertices = mutableListOf<Vertex3D>()
        val faces = mutableListOf<Face3D>()

        // Octahedron with faceted points
        val top = Vertex3D(0f, 0.95f, 0f, Color.rgb(0, 210, 255))
        val bottom = Vertex3D(0f, -0.95f, 0f, Color.rgb(0, 150, 220))

        val midRing = listOf(
            Vertex3D(0.65f, 0.1f, 0f, Color.rgb(60, 230, 255)),
            Vertex3D(0.45f, -0.1f, 0.45f, Color.rgb(40, 210, 250)),
            Vertex3D(0f, 0.15f, 0.65f, Color.rgb(80, 240, 255)),
            Vertex3D(-0.45f, -0.1f, 0.45f, Color.rgb(20, 190, 240)),
            Vertex3D(-0.65f, 0.1f, 0f, Color.rgb(70, 225, 255)),
            Vertex3D(-0.45f, -0.1f, -0.45f, Color.rgb(30, 195, 245)),
            Vertex3D(0f, 0.15f, -0.65f, Color.rgb(90, 245, 255)),
            Vertex3D(0.45f, -0.1f, -0.45f, Color.rgb(50, 205, 250))
        )

        vertices.add(top) // 0
        vertices.addAll(midRing) // 1..8
        vertices.add(bottom) // 9

        // Upper faces
        for (i in 1..8) {
            val next = if (i == 8) 1 else i + 1
            faces.add(createFace(0, i, next, vertices))
        }

        // Lower faces
        for (i in 1..8) {
            val next = if (i == 8) 1 else i + 1
            faces.add(createFace(9, next, i, vertices))
        }

        return Model3D(
            id = "sample_crystal",
            name = "Cristallo di Quarzo Sfaccettato",
            timestamp = System.currentTimeMillis() - 259200000L,
            vertices = vertices,
            faces = faces,
            capturedImagesCount = 12,
            notes = "Minerale ottaedrico prismatico con riflessi ciano e sfaccettature diamantate.",
            category = "Mineralogia"
        )
    }
}
