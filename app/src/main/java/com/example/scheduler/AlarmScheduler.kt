package com.example.scheduler

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.AlarmSettings
import com.example.receiver.AlarmReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"
    private const val ALARM_REQUEST_CODE = 4567

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAlarm(context: Context, settings: AlarmSettings) {
        cancelAlarm(context)

        if (!settings.isEnabled) {
            Log.d(TAG, "Alarm is disabled, skipped scheduling.")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTimeMs = calculateNextTriggerTime(settings)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_HOUR", settings.hour)
            putExtra("ALARM_MINUTE", settings.minute)
        }
        
        // PendingIntent FLAG_MUTABLE is required for AlarmManager Full Screen intents or broad targets on older and modern Android versions
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // If starting Android 12, we must check for schedule exact alarm permission if using setExactAndAllowWhileIdle
                // But setAlarmClock is always allowed for alarm clocks and doesn't require hard checks, making it extremely reliable!
                val info = AlarmManager.AlarmClockInfo(triggerTimeMs, pendingIntent)
                alarmManager.setAlarmClock(info, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
            Log.d(TAG, "Successfully scheduled alarm for: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(triggerTimeMs))}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm exact", e)
            // Fallback to non-exact standard set
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } catch (inner: Exception) {
                Log.e(TAG, "Failure on fallback scheduling", inner)
            }
        }
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Successfully cancelled existing scheduled alarm.")
        }
    }

    fun calculateNextTriggerTime(settings: AlarmSettings): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, settings.hour)
            set(Calendar.MINUTE, settings.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (settings.isWeeklyRepeat) {
            // Find closest repeat day
            val repeatDaysFlags = booleanArrayOf(
                settings.repeatSun, // 1
                settings.repeatMon, // 2
                settings.repeatTue, // 3
                settings.repeatWed, // 4
                settings.repeatThu, // 5
                settings.repeatFri, // 6
                settings.repeatSat  // 7
            )

            // Check if any repeats are selected. If none, treat it as a repeating daily or simple next event
            val noRepeats = repeatDaysFlags.none { it }
            if (noRepeats) {
                if (target.before(now)) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
                return target.timeInMillis
            }

            // Find next closest day
            var daysOffset = 0
            while (daysOffset < 8) {
                val checkCal = Calendar.getInstance().apply {
                    timeInMillis = target.timeInMillis
                    add(Calendar.DAY_OF_YEAR, daysOffset)
                }
                val checkDayOfWeek = checkCal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
                val isSelectedDay = repeatDaysFlags[checkDayOfWeek - 1]

                if (isSelectedDay) {
                    if (checkCal.after(now)) {
                        return checkCal.timeInMillis
                    }
                }
                daysOffset++
            }
            // Fallback fallback setting
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        } else {
            // One-time date specific trigger
            if (settings.oneTimeDate.isNotEmpty()) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                try {
                    val parsedDate = sdf.parse(settings.oneTimeDate)
                    if (parsedDate != null) {
                        val dateCal = Calendar.getInstance().apply {
                            time = parsedDate
                        }
                        target.set(Calendar.YEAR, dateCal.get(Calendar.YEAR))
                        target.set(Calendar.MONTH, dateCal.get(Calendar.MONTH))
                        target.set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing date string ${settings.oneTimeDate}", e)
                }
            }

            // If the specified time has already passed, schedule for tomorrow as fallback
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }
    }
}
