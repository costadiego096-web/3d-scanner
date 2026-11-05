package com.example.model

import android.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class Vector3D(
    val x: Float,
    val y: Float,
    val z: Float
) {
    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun normalized(): Vector3D {
        val len = length()
        return if (len > 0.00001f) {
            Vector3D(x / len, y / len, z / len)
        } else {
            Vector3D(0f, 0f, 1f)
        }
    }

    fun dot(other: Vector3D): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3D): Vector3D {
        return Vector3D(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        )
    }

    operator fun plus(other: Vector3D) = Vector3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3D) = Vector3D(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3D(x * scalar, y * scalar, z * scalar)
}

data class Vertex3D(
    val x: Float,
    val y: Float,
    val z: Float,
    val color: Int = Color.WHITE
) {
    fun toVector(): Vector3D = Vector3D(x, y, z)
}

data class Face3D(
    val v1: Int,
    val v2: Int,
    val v3: Int,
    val normal: Vector3D = Vector3D(0f, 0f, 1f)
)

data class BoundingBox3D(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val minZ: Float,
    val maxZ: Float
) {
    val width: Float get() = maxX - minX
    val height: Float get() = maxY - minY
    val depth: Float get() = maxZ - minZ
    val maxDimension: Float get() = max(width, max(height, depth))

    val centerX: Float get() = (minX + maxX) / 2f
    val centerY: Float get() = (minY + maxY) / 2f
    val centerZ: Float get() = (minZ + maxZ) / 2f
}

data class Model3D(
    val id: String,
    val name: String,
    val timestamp: Long,
    val vertices: List<Vertex3D>,
    val faces: List<Face3D>,
    val capturedImagesCount: Int = 0,
    val notes: String = "",
    val category: String = "Oggetto"
) {
    val vertexCount: Int get() = vertices.size
    val faceCount: Int get() = faces.size

    val boundingBox: BoundingBox3D by lazy {
        if (vertices.isEmpty()) {
            BoundingBox3D(0f, 0f, 0f, 0f, 0f, 0f)
        } else {
            var minX = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            var minZ = Float.MAX_VALUE
            var maxZ = -Float.MAX_VALUE

            for (v in vertices) {
                if (v.x < minX) minX = v.x
                if (v.x > maxX) maxX = v.x
                if (v.y < minY) minY = v.y
                if (v.y > maxY) maxY = v.y
                if (v.z < minZ) minZ = v.z
                if (v.z > maxZ) maxZ = v.z
            }
            BoundingBox3D(minX, maxX, minY, maxY, minZ, maxZ)
        }
    }

    /**
     * Normalized vertices centered around (0,0,0) and scaled to fit within [-1, 1]
     */
    val normalizedVertices: List<Vertex3D> by lazy {
        val bb = boundingBox
        val maxDim = if (bb.maxDimension > 0.0001f) bb.maxDimension else 1f
        val scale = 2f / maxDim
        vertices.map { v ->
            Vertex3D(
                x = (v.x - bb.centerX) * scale,
                y = (v.y - bb.centerY) * scale,
                z = (v.z - bb.centerZ) * scale,
                color = v.color
            )
        }
    }
}
