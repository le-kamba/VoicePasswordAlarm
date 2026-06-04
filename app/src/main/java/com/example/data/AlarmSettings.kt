package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarm_settings")
data class AlarmSettings(
    @PrimaryKey val id: Int = 1,
    val isEnabled: Boolean = false,
    val hour: Int = 7,
    val minute: Int = 0,
    val isWeeklyRepeat: Boolean = false,
    // Days of the week repeat flag (Sunday=Sun, Monday=Mon, etc.)
    val repeatSun: Boolean = false,
    val repeatMon: Boolean = false,
    val repeatTue: Boolean = false,
    val repeatWed: Boolean = false,
    val repeatThu: Boolean = false,
    val repeatFri: Boolean = false,
    val repeatSat: Boolean = false,
    
    // One-time alarm date (used if isWeeklyRepeat is false)
    val oneTimeDate: String = "", // Format: "yyyy-MM-dd"
    
    // Alarm sound choice ("CHIME_SYNTH", "BEEP_PULSE", "ZEN_MEDITATION" or "DEFAULT")
    val alarmSound: String = "CHIME_SYNTH",
    val volume: Float = 0.8f, // 0.0 to 1.0
    val isVibrationEnabled: Boolean = true,
    
    // 3 customizable stop phrases
    val stopPhrase1: String = "おはようございます",
    val stopPhrase2: String = "おきてください",
    val stopPhrase3: String = "アラーム停止",
    
    // Options: "Fix selection" (0, 1, 2) vs "Use random every time" (true/false)
    val selectedPhraseIndex: Int = 0,
    val useRandomPhrase: Boolean = false,
    
    val matchAccuracy: Float = 0.70f // Threshold for matching success, default 70%
)
