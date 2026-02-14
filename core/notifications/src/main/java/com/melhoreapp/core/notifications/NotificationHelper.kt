package com.melhoreapp.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Helper for creating notification channels and showing reminder notifications.
 * Full implementation in Sprint 2.
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

    fun showReminderNotification(
        context: Context,
        reminderId: Long,
        title: String,
        text: String
    ) {
        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        NotificationManagerCompat.from(context).notify(reminderId.toInt(), builder.build())
    }
}
