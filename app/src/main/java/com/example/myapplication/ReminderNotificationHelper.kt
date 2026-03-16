package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat

object ReminderNotificationHelper {

    private const val CHANNEL_ID = "reminder_channel"
    private const val CHANNEL_NAME = "Task Reminders"

    /**
     * Posts a heads-up notification for the given task.
     * Safe to call from any thread.
     *
     * Notification ID strategy: taskId is a numeric counter string in this app, so
     * toIntOrNull() gives zero collision risk. hashCode() is the fallback if IDs ever
     * change to non-numeric strings (acceptable for < 1000 tasks).
     */
    fun notify(context: Context, taskId: String, taskTitle: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        ensureChannel(notificationManager)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val notificationId = taskId.toIntOrNull() ?: taskId.hashCode()

        // FLAG_IMMUTABLE required on API 31+; FLAG_UPDATE_CURRENT prevents stale cached intents
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Task Reminder 🔔")
            .setContentText(taskTitle)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * Creates the notification channel if it does not already exist.
     * Calling createNotificationChannel on an existing channel ID is a no-op on API 26+.
     */
    private fun ensureChannel(notificationManager: NotificationManager) {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) return

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH   // required for lock screen heads-up display
        ).apply {
            setSound(soundUri, audioAttributes)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }
}
