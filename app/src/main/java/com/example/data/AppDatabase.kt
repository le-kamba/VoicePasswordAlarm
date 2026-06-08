package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

@Database(entities = [AlarmSettings::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmSettingsDao(): AlarmSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.d("AppDatabase", "Applying database Migration 1 -> 2")
                try {
                    db.execSQL("ALTER TABLE alarm_settings ADD COLUMN stopPhrase1 TEXT NOT NULL DEFAULT 'おはようございます'")
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Error migrating stopPhrase1 column", e)
                }
                try {
                    db.execSQL("ALTER TABLE alarm_settings ADD COLUMN stopPhrase2 TEXT NOT NULL DEFAULT 'おきてください'")
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Error migrating stopPhrase2 column", e)
                }
                try {
                    db.execSQL("ALTER TABLE alarm_settings ADD COLUMN stopPhrase3 TEXT NOT NULL DEFAULT 'アラーム停止'")
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Error migrating stopPhrase3 column", e)
                }
                try {
                    db.execSQL("ALTER TABLE alarm_settings ADD COLUMN selectedPhraseIndex INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Error migrating selectedPhraseIndex column", e)
                }
                try {
                    db.execSQL("ALTER TABLE alarm_settings ADD COLUMN useRandomPhrase INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Error migrating useRandomPhrase column", e)
                }
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.d("AppDatabase", "Applying database Migration 2 -> 3")
                try {
                    db.execSQL("ALTER TABLE alarm_settings ADD COLUMN requireAllPhrasesRandomOrder INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Error migrating requireAllPhrasesRandomOrder column", e)
                }
                try {
                    db.execSQL("ALTER TABLE alarm_settings ADD COLUMN matchAccuracy REAL NOT NULL DEFAULT 0.70")
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Error migrating matchAccuracy column", e)
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "alarm_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
