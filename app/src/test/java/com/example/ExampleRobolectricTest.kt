package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.Mesh3DEngine
import com.example.engine.ObjExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Scan3D Studio", appName)
    }

    @Test
    fun `mesh generation produces valid model`() {
        val model = Mesh3DEngine.createSampleAmphoraModel()
        assertNotNull(model)
        assertTrue("Vertices should not be empty", model.vertices.isNotEmpty())
        assertTrue("Faces should not be empty", model.faces.isNotEmpty())
        assertTrue("Bounding box dimensions should be positive", model.boundingBox.width > 0f)
        assertEquals(model.vertices.size, model.normalizedVertices.size)
    }

    @Test
    fun `obj exporter generates valid wavefront obj format`() {
        val model = Mesh3DEngine.createSampleCrystalModel()
        val objString = ObjExporter.generateObjString(model)

        assertTrue("Should contain Wavefront OBJ header", objString.contains("# Wavefront OBJ File"))
        assertTrue("Should contain vertices lines", objString.contains("\nv "))
        assertTrue("Should contain normals lines", objString.contains("\nvn "))
        assertTrue("Should contain faces lines", objString.contains("\nf "))
        assertTrue("OBJ faces must be 1-based indexed", !objString.contains("f 0/"))
    }
}
