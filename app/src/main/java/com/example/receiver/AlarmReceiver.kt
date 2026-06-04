package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AlarmReceiver"
        const val CHANNEL_ID = "alarm_channel_id"
        const val NOTIFICATION_ID = 9876
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "AlarmReceiver: Alarm triggered!")

        val alarmIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("ALARM_TRIGGERED", true)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        // Full screen trigger PendingIntent must be an Activity intent
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            5001,
            alarmIntent,
            pendingIntentFlags
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Trigger Screen",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows the alarm trigger screen and sound"
                enableVibration(false) // Let the Activity handle vibration natively
                setSound(null, null) // Let the Activity handle audio play natively
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("音声認識アラーム")
            .setContentText("声を認識してアラームを停止します")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
            Log.d(TAG, "Notification triggered successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display alarm notification", e)
        }

        // Directly launch the activity as well, in case we are allowed to launch immediately
        try {
            context.startActivity(alarmIntent)
        } catch (e: Exception) {
            Log.d(TAG, "Direct launch activity rejected (expected in background on Android 10+): ${e.message}")
        }
    }
}
