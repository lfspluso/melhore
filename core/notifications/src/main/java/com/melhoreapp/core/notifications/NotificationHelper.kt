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
     * @param isRoutine Whether this is a Rotina reminder (affects content intent navigation)
     */
    fun showReminderNotification(
        context: Context,
        reminderId: Long,
        title: String,
        text: String,
        snoozeActions: List<Pair<String, PendingIntent>> = emptyList(),
        isRoutine: Boolean = false
    ) {
        val contentIntent = if (isRoutine) {
            // For Rotina reminders, navigate to task setup screen
            Intent(context, Class.forName("com.melhoreapp.MainActivity")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("navigation_route", "reminders/routine/$reminderId/setup")
            }.let { intent ->
                PendingIntent.getActivity(
                    context,
                    reminderId.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        } else {
            // Regular reminder - open main app
            context.packageManager
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
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
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
    
    /**
     * Shows a completion check notification (for "Fazendo" follow-up) with custom message and actions.
     *
     * @param message The completion check message (e.g. "Você estava fazendo {task name}, você completou?")
     * @param actions List of (label, PendingIntent) for each action (e.g. "Sim", "+15 min", "+1 hora", "Personalizar").
     */
    fun showCompletionCheckNotification(
        context: Context,
        reminderId: Long,
        message: String,
        actions: List<Pair<String, PendingIntent>>
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
            .setContentTitle(message)
            .setContentText("")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        contentIntent?.let { builder.setContentIntent(it) }
        for ((label, pendingIntent) in actions) {
            builder.addAction(
                android.R.drawable.ic_dialog_info,
                label,
                pendingIntent
            )
            // Note: Android typically shows only 3 actions in collapsed state.
            // The 4th action may require expanding the notification on some devices.
        }
        NotificationManagerCompat.from(context).notify(reminderId.toInt(), builder.build())
    }
}
