package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AlarmViewModel
import com.example.ui.ScreenState
import com.example.ui.screens.AlarmActiveScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceRecognitionScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AlarmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Sync and intercept alarm signals on initial launch
        checkTriggerIntent(intent)

        setContent {
            MyApplicationTheme {
                val screenState by viewModel.screenState.collectAsStateWithLifecycle()
                val settings by viewModel.settings.collectAsStateWithLifecycle()
                val recognizedText by viewModel.recognizedText.collectAsStateWithLifecycle()
                val secondsRemaining by viewModel.timeoutSecondsRemaining.collectAsStateWithLifecycle()
                val speechError by viewModel.speechError.collectAsStateWithLifecycle()

                // Request crucial audio and notification credentials on launch
                val multiPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { }

                LaunchedEffect(Unit) {
                    val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    multiPermissionLauncher.launch(permissions.toTypedArray())
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    Crossfade(
                        targetState = screenState,
                        label = "screen_fade_transitions"
                    ) { state ->
                        when (state) {
                            ScreenState.MAIN -> MainScreen(viewModel, settings)
                            ScreenState.SETTINGS -> SettingsScreen(viewModel, settings)
                            ScreenState.ALARM_TRIGGERED -> AlarmActiveScreen(viewModel)
                            ScreenState.VOICE_RECOGNITION -> VoiceRecognitionScreen(
                                viewModel = viewModel,
                                settings = settings,
                                recognizedText = recognizedText,
                                secondsRemaining = secondsRemaining,
                                speechError = speechError
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkTriggerIntent(intent)
    }

    private fun checkTriggerIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("ALARM_TRIGGERED", false) == true) {
            viewModel.triggerAlarm()
        }
    }
}
