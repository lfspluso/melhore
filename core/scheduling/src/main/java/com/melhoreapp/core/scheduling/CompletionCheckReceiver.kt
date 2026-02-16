package com.melhoreapp.core.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.melhoreapp.core.common.preferences.AppPreferences
import com.melhoreapp.core.database.entity.ReminderStatus
import kotlinx.coroutines.runBlocking

/**
 * Receives actions from "Fazendo" follow-up notification.
 * Handles "Sim" (mark as complete), "+15 min", "+1 hora", and "Personalizar" actions.
 */
class CompletionCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId < 0) return
        
        val action = intent.action
        if (action == ACTION_COMPLETE_REMINDER) {
            handleComplete(context, reminderId)
        } else if (action == ACTION_SNOOZE_REMINDER) {
            val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
            val notes = intent.getStringExtra(EXTRA_NOTES) ?: "Reminder"
            val isCustom = intent.getBooleanExtra(EXTRA_IS_CUSTOM, false)
            val rawDuration = intent.getLongExtra(EXTRA_SNOOZE_DURATION_MS, -1L)
            val durationMs = if (isCustom) {
                // For Sprint 14, use default duration (15 min) for "Personalizar"
                15 * 60 * 1000L
            } else if (rawDuration >= 0) {
                rawDuration
            } else {
                AppPreferences(context.applicationContext).getDefaultSnoozeDurationMs()
            }.coerceIn(MIN_SNOOZE_MS, MAX_SNOOZE_MS)
            
            handleSnooze(context, reminderId, title, notes, durationMs)
        }
        
        // Dismiss the follow-up notification
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.cancel(reminderId.toInt())
    }
    
    private fun handleComplete(context: Context, reminderId: Long) {
        val app = context.applicationContext as? SchedulingContext ?: return
        runBlocking {
            val reminder = app.database.reminderDao().getReminderById(reminderId) ?: return@runBlocking
            // Only process if reminder is ACTIVE
            if (reminder.status != ReminderStatus.ACTIVE) return@runBlocking
            
            val now = System.currentTimeMillis()
            val updated = reminder.copy(
                status = ReminderStatus.COMPLETED,
                updatedAt = now
            )
            app.database.reminderDao().update(updated)
            
            // Cancel all scheduled alarms
            app.reminderScheduler.cancelReminder(reminderId)
        }
    }
    
    private fun handleSnooze(context: Context, reminderId: Long, title: String, notes: String, durationMs: Long) {
        val app = context.applicationContext as? SchedulingContext ?: return
        runBlocking {
            val reminder = app.database.reminderDao().getReminderById(reminderId) ?: return@runBlocking
            // Only process if reminder is ACTIVE
            if (reminder.status != ReminderStatus.ACTIVE) return@runBlocking
            
            val now = System.currentTimeMillis()
            val snoozedUntil = now + durationMs
            val updated = reminder.copy(snoozedUntil = snoozedUntil, updatedAt = now)
            app.database.reminderDao().update(updated)
            
            app.reminderScheduler.scheduleReminder(
                reminderId = reminderId,
                triggerAtMillis = snoozedUntil,
                title = title,
                notes = notes.ifEmpty() { "Reminder" },
                isSnoozeFire = true
            )
        }
    }

    companion object {
        const val ACTION_COMPLETE_REMINDER = "com.melhoreapp.core.scheduling.COMPLETE_REMINDER"
        const val ACTION_SNOOZE_REMINDER = "com.melhoreapp.core.scheduling.SNOOZE_REMINDER_FROM_COMPLETION"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_NOTES = "notes"
        const val EXTRA_SNOOZE_DURATION_MS = "snooze_duration_ms"
        const val EXTRA_IS_CUSTOM = "is_custom"
        
        private const val MIN_SNOOZE_MS = 60 * 1000L       // 1 minute
        private const val MAX_SNOOZE_MS = 30 * 24 * 60 * 60 * 1000L  // 30 days
    }
}
