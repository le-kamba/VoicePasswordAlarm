package com.example.ui

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SynthesizedSoundPlayer
import com.example.data.AlarmRepository
import com.example.data.AlarmSettings
import com.example.data.AppDatabase
import com.example.receiver.AlarmReceiver
import com.example.scheduler.AlarmScheduler
import com.example.speech.SpeechRecognizerHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ScreenState {
    MAIN,
    SETTINGS,
    ALARM_TRIGGERED,
    VOICE_RECOGNITION
}

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AlarmRepository
    private val soundPlayer = SynthesizedSoundPlayer()
    private val speechHelper = SpeechRecognizerHelper(application)
    private var vibrator: Vibrator? = null
    private var activeContextRef: java.lang.ref.WeakReference<Context>? = null

    // Room state mapping
    private val _settings = MutableStateFlow(AlarmSettings())
    val settings: StateFlow<AlarmSettings> = _settings.asStateFlow()

    // Screen State
    private val _screenState = MutableStateFlow(ScreenState.MAIN)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    // Test mode tracker
    private val _isTestMode = MutableStateFlow(false)
    val isTestMode: StateFlow<Boolean> = _isTestMode.asStateFlow()

    // Real-time voice inputs
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _speechError = MutableStateFlow<String?>(null)
    val speechError: StateFlow<String?> = _speechError.asStateFlow()

    private val _timeoutSecondsRemaining = MutableStateFlow(60)
    val timeoutSecondsRemaining: StateFlow<Int> = _timeoutSecondsRemaining.asStateFlow()

    // Resolved active stopping phrase for the current session
    private val _activeSessionPhrase = MutableStateFlow("おはようございます")
    val activeSessionPhrase: StateFlow<String> = _activeSessionPhrase.asStateFlow()

    private var timeoutJob: Job? = null
    private var isAlarmRunning = false

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AlarmRepository(application, database.alarmSettingsDao())
        
        // Load settings reactively
        viewModelScope.launch {
            repository.alarmSettings.collectLatest {
                _settings.value = it
            }
        }
    }

    // Handles alarm trigger
    fun triggerAlarm(isTest: Boolean = false) {
        if (isAlarmRunning) return
        isAlarmRunning = true
        _isTestMode.value = isTest
        _screenState.value = ScreenState.ALARM_TRIGGERED
        
        // Resolve target phrase for this active session
        resolveActiveSessionPhrase()
        
        // Play sound & vibration based on latest settings
        val current = _settings.value
        soundPlayer.startPlaying(current.alarmSound, current.volume)
        if (current.isVibrationEnabled) {
            startVibration()
        }
    }

    // Opens Voice Recognition with foreground Context
    fun startVoiceRecognition(context: Context) {
        activeContextRef = java.lang.ref.WeakReference(context)
        _screenState.value = ScreenState.VOICE_RECOGNITION
        _recognizedText.value = ""
        _speechError.value = null
        
        // Ensure session phrase is resolved before listening
        resolveActiveSessionPhrase()
        
        // Lower alarm sound volume ("止めるための音声認識画面ではアラーム音は小さくし")
        val current = _settings.value
        soundPlayer.setPlayingVolume(current.volume * 0.15f)
        
        // Stop vibration ("バイブは止めてください")
        stopVibration()

        // Setup real-time listening
        startSpeechListeningSession()

        // Start 60-second timeout countdown ("音声入力が1分間行われなかった場合自動的に戻る")
        startTimeoutCountdown()
    }

    fun resolveActiveSessionPhrase() {
        val s = _settings.value
        val list = listOf(s.stopPhrase1, s.stopPhrase2, s.stopPhrase3).filter { it.isNotBlank() }
        if (s.useRandomPhrase) {
            _activeSessionPhrase.value = if (list.isNotEmpty()) list.random() else "おはようございます"
        } else {
            val chosen = when (s.selectedPhraseIndex) {
                0 -> s.stopPhrase1
                1 -> s.stopPhrase2
                2 -> s.stopPhrase3
                else -> s.stopPhrase1
            }
            _activeSessionPhrase.value = if (chosen.isNotBlank()) chosen else (list.firstOrNull() ?: "おはようございます")
        }
    }

    private fun startSpeechListeningSession() {
        val context = activeContextRef?.get() ?: getApplication<Application>()
        speechHelper.startListening(context, object : SpeechRecognizerHelper.SpeechListener {
            override fun onReady() {
                _speechError.value = null
            }

            override fun onPartialResult(text: String) {
                if (text.isNotEmpty()) {
                    _recognizedText.value = text
                    // Reset timeout since speech was received ("音声入力が行われた")
                    resetTimeoutTimer()
                    checkVoiceMatch(text)
                }
            }

            override fun onFinalResult(text: String) {
                if (text.isNotEmpty()) {
                    _recognizedText.value = text
                    resetTimeoutTimer()
                    checkVoiceMatch(text)
                }
            }

            override fun onError(errorMsg: String) {
                _speechError.value = errorMsg
                // Automatically restart listening to allow fluid continuous speak tries, unless we exited the screen
                if (_screenState.value == ScreenState.VOICE_RECOGNITION) {
                    viewModelScope.launch {
                        delay(2000)
                        if (_screenState.value == ScreenState.VOICE_RECOGNITION) {
                            startSpeechListeningSession()
                        }
                    }
                }
            }
        })
    }

    // Checks similarity between speech and stop target phrase
    private fun checkVoiceMatch(text: String) {
        val target = _activeSessionPhrase.value
        val accuracyThreshold = _settings.value.matchAccuracy
        val similarity = calculateSimilarity(text, target)
        
        Log.d("AlarmViewModel", "Comparing '$text' with '$target'. Quality score: $similarity (Target: $accuracyThreshold)")

        if (similarity >= accuracyThreshold) {
            // Dismiss alarm completely!
            stopAlarmEntirely()
        }
    }

    // String fuzzy Levenshtein and containment similarity algorithm
    private fun calculateSimilarity(s1: String, s2: String): Float {
        val str1 = s1.lowercase().trim().replace("\\s".toRegex(), "")
        val str2 = s2.lowercase().trim().replace("\\s".toRegex(), "")
        if (str1 == str2) return 1.0f
        if (str1.isEmpty() || str2.isEmpty()) return 0.0f
        
        // 1. Direct Containment Match is highest confidence (e.g. saying "おっけーアラーム停止" when stopping contains "アラーム停止")
        if (str1.contains(str2) || str2.contains(str1)) {
            return 0.95f
        }

        // 2. Levenshtein edit distance for phonetic minor variations
        val dp = IntArray(str2.length + 1) { it }
        for (i in 1..str1.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..str2.length) {
                val temp = dp[j]
                if (str1[i - 1] == str2[j - 1]) {
                    dp[j] = prev
                } else {
                    dp[j] = minOf(dp[j - 1] + 1, dp[j] + 1, prev + 1)
                }
                prev = temp
            }
        }
        val distance = dp[str2.length]
        val maxLength = maxOf(str1.length, str2.length)
        return 1.0f - (distance.toFloat() / maxLength.toFloat())
    }

    // 1-minute countdown mechanism
    private fun startTimeoutCountdown() {
        timeoutJob?.cancel()
        _timeoutSecondsRemaining.value = 60
        timeoutJob = viewModelScope.launch {
            while (_timeoutSecondsRemaining.value > 0) {
                delay(1000)
                _timeoutSecondsRemaining.value -= 1
            }
            // Timeout expired! Automatic return back to basic Alarm triggered screen with loud sound & vibrate
            returnToAlarmFiredScreenOnTimeout()
        }
    }

    private fun resetTimeoutTimer() {
        _timeoutSecondsRemaining.value = 60
    }

    private fun returnToAlarmFiredScreenOnTimeout() {
        activeContextRef = null
        speechHelper.stopListening()
        timeoutJob?.cancel()
        _screenState.value = ScreenState.ALARM_TRIGGERED
        
        // Restore volume & vibrations
        val current = _settings.value
        soundPlayer.setPlayingVolume(current.volume)
        if (current.isVibrationEnabled) {
            startVibration()
        }
    }

    // Stops the alarm and dismisses notifications
    private fun stopAlarmEntirely() {
        isAlarmRunning = false
        _isTestMode.value = false
        activeContextRef = null
        speechHelper.stopListening()
        timeoutJob?.cancel()
        soundPlayer.stopPlaying()
        stopVibration()
        
        _screenState.value = ScreenState.MAIN

        // Cancel notification if shown
        val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(AlarmReceiver.NOTIFICATION_ID)

        // Reschedule alarm for next period automatically so it functions forever repeat (No snooze)
        viewModelScope.launch {
            // Re-read and write to keep AlarmManager updated with safety
            repository.saveAlarmSettings(_settings.value)
        }
    }

    // Helper functions for updating Alarm Configuration
    fun updateAlarmTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val updated = _settings.value.copy(hour = hour, minute = minute)
            repository.saveAlarmSettings(updated)
        }
    }

    fun updateWeeklyRepeat(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _settings.value.copy(isWeeklyRepeat = enabled)
            repository.saveAlarmSettings(updated)
        }
    }

    fun updateRepeatDay(dayIndex: Int, checked: Boolean) {
        viewModelScope.launch {
            val current = _settings.value
            val updated = when (dayIndex) {
                0 -> current.copy(repeatSun = checked)
                1 -> current.copy(repeatMon = checked)
                2 -> current.copy(repeatTue = checked)
                3 -> current.copy(repeatWed = checked)
                4 -> current.copy(repeatThu = checked)
                5 -> current.copy(repeatFri = checked)
                else -> current.copy(repeatSat = checked)
            }
            repository.saveAlarmSettings(updated)
        }
    }

    fun updateOneTimeDate(dateStr: String) {
        viewModelScope.launch {
            val updated = _settings.value.copy(oneTimeDate = dateStr)
            repository.saveAlarmSettings(updated)
        }
    }

    fun updateAlarmSound(soundType: String) {
        viewModelScope.launch {
            val updated = _settings.value.copy(alarmSound = soundType)
            repository.saveAlarmSettings(updated)
        }
    }

    fun updateVolume(volume: Float) {
        viewModelScope.launch {
            val updated = _settings.value.copy(volume = volume)
            repository.saveAlarmSettings(updated)
        }
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _settings.value.copy(isVibrationEnabled = enabled)
            repository.saveAlarmSettings(updated)
        }
    }

    fun updateStopPhrase(index: Int, phrase: String) {
        viewModelScope.launch {
            val current = _settings.value
            val updated = when (index) {
                0 -> current.copy(stopPhrase1 = phrase)
                1 -> current.copy(stopPhrase2 = phrase)
                2 -> current.copy(stopPhrase3 = phrase)
                else -> current
            }
            repository.saveAlarmSettings(updated)
        }
    }

    fun deleteStopPhrase(index: Int) {
        viewModelScope.launch {
            val current = _settings.value
            val updated = when (index) {
                0 -> current.copy(stopPhrase1 = "")
                1 -> current.copy(stopPhrase2 = "")
                2 -> current.copy(stopPhrase3 = "")
                else -> current
            }
            // Auto-adjust selectedPhraseIndex if needed
            val adjusted = if (updated.selectedPhraseIndex == index) {
                val nextIndex = listOf(0, 1, 2).find { idx ->
                    when (idx) {
                        0 -> updated.stopPhrase1.isNotBlank()
                        1 -> updated.stopPhrase2.isNotBlank()
                        2 -> updated.stopPhrase3.isNotBlank()
                        else -> false
                    }
                } ?: 0
                updated.copy(selectedPhraseIndex = nextIndex)
            } else {
                updated
            }
            repository.saveAlarmSettings(adjusted)
        }
    }

    fun updateSelectedPhraseIndex(index: Int) {
        viewModelScope.launch {
            val updated = _settings.value.copy(selectedPhraseIndex = index, useRandomPhrase = false)
            repository.saveAlarmSettings(updated)
        }
    }

    fun updateUseRandomPhrase(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _settings.value.copy(useRandomPhrase = enabled)
            repository.saveAlarmSettings(updated)
        }
    }

    fun updateMatchAccuracy(accuracy: Float) {
        viewModelScope.launch {
            val updated = _settings.value.copy(matchAccuracy = accuracy)
            repository.saveAlarmSettings(updated)
        }
    }

    fun toggleAlarmEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _settings.value.copy(isEnabled = enabled)
            repository.saveAlarmSettings(updated)
        }
    }

    fun navigateTo(state: ScreenState) {
        _screenState.value = state
        if (state == ScreenState.MAIN || state == ScreenState.SETTINGS) {
            // Clean up sounding alarms if navigating away manually
            if (isAlarmRunning) {
                stopAlarmEntirely()
            }
        }
    }

    // Low level vibrations control
    @Suppress("DEPRECATION")
    private fun startVibration() {
        if (vibrator == null) {
            vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        val pattern = longArrayOf(0, 1000, 500, 1000, 500)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("AlarmViewModel", "Failed to start vibration", e)
        }
    }

    private fun stopVibration() {
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("AlarmViewModel", "Failed to cancel vibration", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundPlayer.stopPlaying()
        stopVibration()
        speechHelper.stopListening()
        timeoutJob?.cancel()
    }

    // Allows manual keyboard-simulation test input for speech recognition (excellent for testing/emulators)
    fun simulateVoiceSpeechInput(simulatedText: String) {
        if (_screenState.value == ScreenState.VOICE_RECOGNITION) {
            _recognizedText.value = simulatedText
            resetTimeoutTimer()
            checkVoiceMatch(simulatedText)
        }
    }
}
