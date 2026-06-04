package com.example.data

import android.content.Context
import com.example.scheduler.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class AlarmRepository(
    private val context: Context,
    private val dao: AlarmSettingsDao
) {
    val alarmSettings: Flow<AlarmSettings> = dao.getAlarmSettingsFlow()
        .map { it ?: AlarmSettings() }
        .distinctUntilChanged()

    suspend fun getAlarmSettingsSync(): AlarmSettings {
        return dao.getAlarmSettings() ?: AlarmSettings()
    }

    suspend fun saveAlarmSettings(settings: AlarmSettings) {
        dao.saveAlarmSettings(settings)
        AlarmScheduler.scheduleAlarm(context, settings)
    }

    suspend fun toggleAlarm(isEnabled: Boolean) {
        val current = getAlarmSettingsSync()
        val updated = current.copy(isEnabled = isEnabled)
        saveAlarmSettings(updated)
    }
}
