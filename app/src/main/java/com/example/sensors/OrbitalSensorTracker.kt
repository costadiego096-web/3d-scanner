package com.example.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class SensorOrientationState(
    val azimuthDegrees: Float = 0f, // 0 - 360
    val pitchDegrees: Float = 0f,   // -90 to +90
    val rollDegrees: Float = 0f,    // -180 to +180
    val currentSector: Int = 0,     // 0 to 15 (16 sectors around object)
    val isStable: Boolean = true,
    val capturedSectors: Set<Int> = emptySet(),
    val totalSectors: Int = 16
)

class OrbitalSensorTracker(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _orientationState = MutableStateFlow(SensorOrientationState())
    val orientationState: StateFlow<SensorOrientationState> = _orientationState.asStateFlow()

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val lastAcc = FloatArray(3)
    private val lastMag = FloatArray(3)
    private var hasAcc = false
    private var hasMag = false

    private var lastAzimuth = 0f
    private var lastPitch = 0f
    private var isTracking = false

    fun start() {
        if (isTracking) return
        isTracking = true

        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    fun stop() {
        if (!isTracking) return
        isTracking = false
        sensorManager.unregisterListener(this)
    }

    fun markSectorCaptured(sectorIndex: Int) {
        val currentSet = _orientationState.value.capturedSectors
        _orientationState.value = _orientationState.value.copy(
            capturedSectors = currentSet + sectorIndex
        )
    }

    fun resetCapturedSectors() {
        _orientationState.value = _orientationState.value.copy(
            capturedSectors = emptySet()
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                updateAngles(orientationAngles)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, lastAcc, 0, 3)
                hasAcc = true
                if (hasMag) {
                    computeOrientationFromAccMag()
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, lastMag, 0, 3)
                hasMag = true
                if (hasAcc) {
                    computeOrientationFromAccMag()
                }
            }
        }
    }

    private fun computeOrientationFromAccMag() {
        if (SensorManager.getRotationMatrix(rotationMatrix, null, lastAcc, lastMag)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            updateAngles(orientationAngles)
        }
    }

    private fun updateAngles(angles: FloatArray) {
        // angles[0]: azimuth in radians [-PI, PI]
        // angles[1]: pitch in radians [-PI/2, PI/2]
        // angles[2]: roll in radians [-PI, PI]

        var azimuthDeg = Math.toDegrees(angles[0].toDouble()).toFloat()
        if (azimuthDeg < 0) azimuthDeg += 360f

        val pitchDeg = Math.toDegrees(angles[1].toDouble()).toFloat()
        val rollDeg = Math.toDegrees(angles[2].toDouble()).toFloat()

        // Detect stability: rate of change
        val deltaAzimuth = abs(azimuthDeg - lastAzimuth)
        val deltaPitch = abs(pitchDeg - lastPitch)
        val isStable = deltaAzimuth < 3.5f && deltaPitch < 3.5f

        lastAzimuth = azimuthDeg
        lastPitch = pitchDeg

        // 16 sectors around 360 degrees -> 22.5 deg per sector
        val sectorSize = 360f / 16f
        val currentSector = ((azimuthDeg / sectorSize).toInt()) % 16

        _orientationState.value = _orientationState.value.copy(
            azimuthDegrees = azimuthDeg,
            pitchDegrees = pitchDeg,
            rollDegrees = rollDeg,
            currentSector = currentSector,
            isStable = isStable
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
