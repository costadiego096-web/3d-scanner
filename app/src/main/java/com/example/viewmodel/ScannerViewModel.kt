package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.CapturedFrame
import com.example.engine.Mesh3DEngine
import com.example.model.Model3D
import com.example.sensors.OrbitalSensorTracker
import com.example.sensors.SensorOrientationState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppScreen {
    LIBRARY,
    SCANNER,
    RECONSTRUCTION,
    VIEWER
}

data class ReconstructionProgressState(
    val stage: Int = 1,
    val progress: Float = 0f, // 0.0 to 1.0
    val statusText: String = "",
    val pointsCount: Int = 0
)

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    val sensorTracker = OrbitalSensorTracker(application)

    private val _currentScreen = MutableStateFlow(AppScreen.LIBRARY)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _models = MutableStateFlow<List<Model3D>>(emptyList())
    val models: StateFlow<List<Model3D>> = _models.asStateFlow()

    private val _selectedModel = MutableStateFlow<Model3D?>(null)
    val selectedModel: StateFlow<Model3D?> = _selectedModel.asStateFlow()

    private val _capturedFrames = MutableStateFlow<List<CapturedFrame>>(emptyList())
    val capturedFrames: StateFlow<List<CapturedFrame>> = _capturedFrames.asStateFlow()

    private val _isAutoCaptureEnabled = MutableStateFlow(true)
    val isAutoCaptureEnabled: StateFlow<Boolean> = _isAutoCaptureEnabled.asStateFlow()

    private val _reconstructionState = MutableStateFlow(ReconstructionProgressState())
    val reconstructionState: StateFlow<ReconstructionProgressState> = _reconstructionState.asStateFlow()

    val sensorState: StateFlow<SensorOrientationState> = sensorTracker.orientationState

    private var lastAutoCaptureSector: Int = -1

    init {
        // Preload curated 3D models so user can immediately view & export
        val defaultModels = listOf(
            Mesh3DEngine.createSampleAmphoraModel(),
            Mesh3DEngine.createSampleTurbineModel(),
            Mesh3DEngine.createSampleCrystalModel()
        )
        _models.value = defaultModels
        _selectedModel.value = defaultModels.first()
    }

    fun navigateTo(screen: AppScreen) {
        if (screen == AppScreen.SCANNER) {
            sensorTracker.start()
        } else {
            sensorTracker.stop()
        }
        _currentScreen.value = screen
    }

    fun handleBackPress(): Boolean {
        return when (_currentScreen.value) {
            AppScreen.VIEWER -> {
                _currentScreen.value = AppScreen.LIBRARY
                true
            }
            AppScreen.SCANNER -> {
                sensorTracker.stop()
                _currentScreen.value = AppScreen.LIBRARY
                true
            }
            AppScreen.RECONSTRUCTION -> {
                _currentScreen.value = AppScreen.SCANNER
                true
            }
            AppScreen.LIBRARY -> false
        }
    }

    fun selectModel(model: Model3D) {
        _selectedModel.value = model
        navigateTo(AppScreen.VIEWER)
    }

    fun deleteModel(modelId: String) {
        _models.value = _models.value.filter { it.id != modelId }
        if (_selectedModel.value?.id == modelId) {
            _selectedModel.value = _models.value.firstOrNull()
        }
    }

    fun toggleAutoCapture() {
        _isAutoCaptureEnabled.value = !_isAutoCaptureEnabled.value
    }

    fun startNewScan() {
        _capturedFrames.value = emptyList()
        sensorTracker.resetCapturedSectors()
        lastAutoCaptureSector = -1
        navigateTo(AppScreen.SCANNER)
    }

    fun addCapturedFrame(bitmap: Bitmap) {
        val sState = sensorState.value
        val frame = CapturedFrame(
            bitmap = bitmap,
            azimuthDegrees = sState.azimuthDegrees,
            pitchDegrees = sState.pitchDegrees
        )
        _capturedFrames.value = _capturedFrames.value + frame
        sensorTracker.markSectorCaptured(sState.currentSector)
        lastAutoCaptureSector = sState.currentSector
    }

    fun checkAutoCapture(onTriggerCapture: () -> Unit) {
        if (!_isAutoCaptureEnabled.value) return
        val sState = sensorState.value
        val sector = sState.currentSector

        if (sState.isStable && !sState.capturedSectors.contains(sector) && sector != lastAutoCaptureSector) {
            lastAutoCaptureSector = sector
            onTriggerCapture()
        }
    }

    fun processAndReconstruct(scanName: String = "Scansione 3D #${_models.value.size + 1}") {
        navigateTo(AppScreen.RECONSTRUCTION)

        viewModelScope.launch {
            val frames = _capturedFrames.value

            // Stage 1: Feature Extraction
            _reconstructionState.value = ReconstructionProgressState(
                stage = 1,
                progress = 0.25f,
                statusText = "Estrazione punti salienti e corrispondenza fotogrammi...",
                pointsCount = frames.size * 45
            )
            delay(900)

            // Stage 2: Dense Point Cloud
            _reconstructionState.value = ReconstructionProgressState(
                stage = 2,
                progress = 0.55f,
                statusText = "Generazione nuvola di punti densa tridimensionale...",
                pointsCount = frames.size * 120 + 240
            )
            delay(900)

            // Stage 3: Surface Triangulation
            _reconstructionState.value = ReconstructionProgressState(
                stage = 3,
                progress = 0.85f,
                statusText = "Triangolazione mesh di superficie e calcolo normali...",
                pointsCount = frames.size * 180 + 380
            )
            delay(900)

            // Reconstruct model
            val newModel = Mesh3DEngine.reconstructModelFromFrames(
                name = scanName,
                frames = frames
            )

            // Stage 4: Completed
            _reconstructionState.value = ReconstructionProgressState(
                stage = 4,
                progress = 1.0f,
                statusText = "Mesh 3D completata e pronta per l'esportazione OBJ!",
                pointsCount = newModel.vertexCount
            )
            delay(400)

            _models.value = listOf(newModel) + _models.value
            _selectedModel.value = newModel
            navigateTo(AppScreen.VIEWER)
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorTracker.stop()
    }
}
