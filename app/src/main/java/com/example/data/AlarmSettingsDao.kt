package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmSettingsDao {
    @Query("SELECT * FROM alarm_settings WHERE id = 1 LIMIT 1")
    fun getAlarmSettingsFlow(): Flow<AlarmSettings?>

    @Query("SELECT * FROM alarm_settings WHERE id = 1 LIMIT 1")
    suspend fun getAlarmSettings(): AlarmSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAlarmSettings(settings: AlarmSettings)
}
