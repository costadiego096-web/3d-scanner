package com.example

import com.example.model.Vector3D
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun vectorOperations_areCorrect() {
        val v1 = Vector3D(1f, 0f, 0f)
        val v2 = Vector3D(0f, 1f, 0f)

        // Dot product of orthogonal vectors
        assertEquals(0f, v1.dot(v2), 0.0001f)

        // Cross product of X and Y gives Z
        val cross = v1.cross(v2)
        assertEquals(0f, cross.x, 0.0001f)
        assertEquals(0f, cross.y, 0.0001f)
        assertEquals(1f, cross.z, 0.0001f)

        // Normalization
        val v3 = Vector3D(3f, 0f, 4f)
        val norm = v3.normalized()
        assertEquals(1f, norm.length(), 0.0001f)
        assertEquals(0.6f, norm.x, 0.0001f)
        assertEquals(0.8f, norm.z, 0.0001f)
    }
}
