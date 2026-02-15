package com.melhoreapp.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Helper for creating notification channels and showing reminder notifications.
 * Tapping a reminder notification opens the app (launcher activity).
 */
object NotificationHelper {
    const val CHANNEL_REMINDERS = "reminders"
    const val CHANNEL_SNOOZED = "snoozed"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_REMINDERS,
                    context.getString(R.string.channel_reminders_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = context.getString(R.string.channel_reminders_desc) }
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SNOOZED,
                    context.getString(R.string.channel_snoozed_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = context.getString(R.string.channel_snoozed_desc) }
            )
        }
    }

    /**
     * Shows a reminder notification with optional snooze actions.
     *
     * @param snoozeActions List of (label, PendingIntent) for each snooze option (e.g. "5 min", "1 hour").
     *                     The receiver for each PendingIntent should read the chosen duration from the intent.
     */
    fun showReminderNotification(
        context: Context,
        reminderId: Long,
        title: String,
        text: String,
        snoozeActions: List<Pair<String, PendingIntent>> = emptyList()
    ) {
        val contentIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.let { intent ->
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                PendingIntent.getActivity(
                    context,
                    reminderId.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        contentIntent?.let { builder.setContentIntent(it) }
        for ((label, pendingIntent) in snoozeActions) {
            builder.addAction(
                android.R.drawable.ic_dialog_info,
                label,
                pendingIntent
            )
        }
        NotificationManagerCompat.from(context).notify(reminderId.toInt(), builder.build())
    }
}
