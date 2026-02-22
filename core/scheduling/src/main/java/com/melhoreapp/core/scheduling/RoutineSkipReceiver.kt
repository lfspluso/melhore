package com.melhoreapp.core.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.melhoreapp.core.database.entity.ReminderStatus
import kotlinx.coroutines.runBlocking

/**
 * Receives "Skip day" action from Rotina notification.
 * Cancels all alarms for the Rotina, then advances to next occurrence and reschedules (Sprint 21.1).
 */
class RoutineSkipReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SKIP_ROUTINE_DAY) return
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId < 0) return

        val app = context.applicationContext as? SchedulingContext ?: return
        try {
            runBlocking {
                val reminder = app.database.reminderDao().getReminderById(reminderId) ?: return@runBlocking
                // Only process if reminder is ACTIVE and is a Rotina
                if (reminder.status != ReminderStatus.ACTIVE || !reminder.isRoutine) return@runBlocking

                // Cancel all alarms (main and 30-min) before rescheduling (Sprint 21.1)
                app.reminderScheduler.cancelReminder(reminderId)

                val now = System.currentTimeMillis()
                // Calculate next occurrence
                val nextDue = nextOccurrenceMillis(
                    reminder.dueAt,
                    reminder.type,
                    reminder.customRecurrenceDays
                ) ?: return@runBlocking

                // Update reminder with next occurrence
                val updated = reminder.copy(dueAt = nextDue, updatedAt = now)
                app.database.reminderDao().update(updated)

                // Reschedule next occurrence only
                app.reminderScheduler.scheduleReminder(
                    reminderId = reminderId,
                    triggerAtMillis = nextDue,
                    title = updated.title,
                    notes = updated.notes.ifEmpty() { "Reminder" },
                    isSnoozeFire = false
                )

                // Cancel current notification
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                manager.cancel(reminderId.toInt())
            }
        } catch (e: Exception) {
            Log.e(TAG, "RoutineSkipReceiver failed for reminderId=$reminderId", e)
        }
    }

    companion object {
        private const val TAG = "RoutineSkipReceiver"
        const val ACTION_SKIP_ROUTINE_DAY = "com.melhoreapp.core.scheduling.SKIP_ROUTINE_DAY"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
