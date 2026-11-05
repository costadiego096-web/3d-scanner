package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ModelViewerScreen
import com.example.ui.screens.ReconstructionScreen
import com.example.ui.screens.ScanLibraryScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.ScannerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0D1117)
                ) {
                    Scan3DApp()
                }
            }
        }
    }
}

@Composable
fun Scan3DApp(
    viewModel: ScannerViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val models by viewModel.models.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()

    // Handle Android system back gesture / button
    BackHandler(enabled = currentScreen != AppScreen.LIBRARY) {
        viewModel.handleBackPress()
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            AppScreen.LIBRARY -> {
                ScanLibraryScreen(
                    models = models,
                    onStartScan = { viewModel.startNewScan() },
                    onSelectModel = { model -> viewModel.selectModel(model) },
                    onDeleteModel = { id -> viewModel.deleteModel(id) }
                )
            }

            AppScreen.SCANNER -> {
                ScannerScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.handleBackPress() }
                )
            }

            AppScreen.RECONSTRUCTION -> {
                ReconstructionScreen(viewModel = viewModel)
            }

            AppScreen.VIEWER -> {
                selectedModel?.let { model ->
                    ModelViewerScreen(
                        model = model,
                        onBack = { viewModel.handleBackPress() }
                    )
                } ?: run {
                    ScanLibraryScreen(
                        models = models,
                        onStartScan = { viewModel.startNewScan() },
                        onSelectModel = { model -> viewModel.selectModel(model) },
                        onDeleteModel = { id -> viewModel.deleteModel(id) }
                    )
                }
            }
        }
    }
}
