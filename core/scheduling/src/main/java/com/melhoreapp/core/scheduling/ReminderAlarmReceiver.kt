package com.melhoreapp.core.scheduling

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.melhoreapp.core.database.entity.RecurrenceType
import com.melhoreapp.core.notifications.NotificationHelper
import com.melhoreapp.core.notifications.R
import kotlinx.coroutines.runBlocking

/**
 * Receives exact-alarm intents from AlarmManager and shows the reminder notification.
 * For recurring reminders, computes next occurrence, updates DB, and reschedules.
 * When the alarm was a snooze fire (EXTRA_IS_SNOOZE_FIRE), clears snoozedUntil then reschedules if recurring.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId < 0) return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
        val notes = intent.getStringExtra(EXTRA_NOTES) ?: "Reminder"
        val isSnoozeFire = intent.getBooleanExtra(EXTRA_IS_SNOOZE_FIRE, false)

        val appContext = context.applicationContext
        val snoozeLabelResIds = listOf(
            R.string.snooze_5_min,
            R.string.snooze_15_min,
            R.string.snooze_1_hour,
            R.string.snooze_1_day
        )
        val snoozeActions = SNOOZE_DURATIONS_MS.mapIndexed { index, durationMs ->
            val snoozeIntent = Intent(appContext, SnoozeReceiver::class.java).apply {
                action = SnoozeReceiver.ACTION_SNOOZE_REMINDER
                putExtra(SnoozeReceiver.EXTRA_REMINDER_ID, reminderId)
                putExtra(SnoozeReceiver.EXTRA_TITLE, title)
                putExtra(SnoozeReceiver.EXTRA_NOTES, notes)
                putExtra(SnoozeReceiver.EXTRA_SNOOZE_DURATION_MS, durationMs)
            }
            val pending = PendingIntent.getBroadcast(
                appContext,
                reminderId.toInt() + SNOOZE_REQUEST_CODE_OFFSET + index,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            appContext.getString(snoozeLabelResIds[index]) to pending
        }
        NotificationHelper.showReminderNotification(
            appContext,
            reminderId,
            title,
            notes,
            snoozeActions
        )

        val app = context.applicationContext as? SchedulingContext ?: return
        runBlocking {
            val reminder = app.database.reminderDao().getReminderById(reminderId) ?: return@runBlocking
            if (!reminder.isActive) return@runBlocking

            val now = System.currentTimeMillis()
            var updated = reminder

            if (isSnoozeFire) {
                updated = reminder.copy(snoozedUntil = null, updatedAt = now)
                app.database.reminderDao().update(updated)
            }

            if (updated.type == RecurrenceType.DAILY || updated.type == RecurrenceType.WEEKLY) {
                val nextDue = nextOccurrenceMillis(updated.dueAt, updated.type) ?: return@runBlocking
                val nextEntity = updated.copy(dueAt = nextDue, updatedAt = now)
                app.database.reminderDao().update(nextEntity)
                app.reminderScheduler.scheduleReminder(
                    reminderId = reminderId,
                    triggerAtMillis = nextDue,
                    title = nextEntity.title,
                    notes = nextEntity.notes.ifEmpty() { "Reminder" },
                    isSnoozeFire = false
                )
            }
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_NOTES = "notes"
        const val EXTRA_IS_SNOOZE_FIRE = "is_snooze_fire"
    }
}
