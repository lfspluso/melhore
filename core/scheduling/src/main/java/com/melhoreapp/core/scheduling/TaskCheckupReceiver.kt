package com.melhoreapp.core.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.melhoreapp.core.database.entity.ReminderStatus
import kotlinx.coroutines.runBlocking

/**
 * Receives checkup notification actions for task reminders.
 * Handles "Done", "Snooze", and "Continue" actions.
 */
class TaskCheckupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId < 0) return

        val action = intent.action
        val app = context.applicationContext as? SchedulingContext ?: return

        runBlocking {
            val reminder = app.database.reminderDao().getReminderById(reminderId) ?: return@runBlocking
            // Only process if reminder is ACTIVE and is a task
            if (reminder.status != ReminderStatus.ACTIVE || !reminder.isTask) return@runBlocking

            when (action) {
                ACTION_DONE_TASK -> handleDone(app, reminder)
                ACTION_CONTINUE_TASK -> handleContinue(app, reminder)
                // Snooze is handled by SnoozeReceiver, but we cancel the notification here
            }

            // Cancel current notification
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.cancel(reminderId.toInt())
        }
    }

    private fun handleDone(app: SchedulingContext, reminder: com.melhoreapp.core.database.entity.ReminderEntity) {
        runBlocking {
            val now = System.currentTimeMillis()
            val updated = reminder.copy(
                status = ReminderStatus.COMPLETED,
                updatedAt = now
            )
            app.database.reminderDao().update(updated)

            // Cancel all scheduled alarms
            app.reminderScheduler.cancelReminder(reminder.id)
        }
    }

    private fun handleContinue(app: SchedulingContext, reminder: com.melhoreapp.core.database.entity.ReminderEntity) {
        runBlocking {
            val checkupFrequencyHours = reminder.checkupFrequencyHours ?: return@runBlocking
            if (checkupFrequencyHours <= 0) return@runBlocking

            val now = System.currentTimeMillis()
            val nextCheckupTime = now + (checkupFrequencyHours * 60 * 60 * 1000L)

            // Schedule next checkup notification
            app.reminderScheduler.scheduleReminder(
                reminderId = reminder.id,
                triggerAtMillis = nextCheckupTime,
                title = reminder.title,
                notes = reminder.notes.ifEmpty() { "Reminder" },
                isSnoozeFire = false,
                isTaskCheckup = true
            )
        }
    }

    companion object {
        const val ACTION_DONE_TASK = "com.melhoreapp.core.scheduling.DONE_TASK"
        const val ACTION_CONTINUE_TASK = "com.melhoreapp.core.scheduling.CONTINUE_TASK"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
