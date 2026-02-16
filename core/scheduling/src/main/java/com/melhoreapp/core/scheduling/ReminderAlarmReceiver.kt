package com.melhoreapp.core.scheduling

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.melhoreapp.core.common.preferences.AppPreferences
import com.melhoreapp.core.database.entity.RecurrenceType
import com.melhoreapp.core.database.entity.ReminderStatus
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
        val isFazendoFollowup = intent.getBooleanExtra(EXTRA_IS_FAZENDO_FOLLOWUP, false)

        val appContext = context.applicationContext
        
        // Handle "Fazendo" follow-up notification
        if (isFazendoFollowup) {
            showCompletionCheckNotification(context, reminderId, title, notes)
            return
        }
        
        // Regular reminder notification with new snooze options
        val snoozeActions = buildSnoozeActions(appContext, reminderId, title, notes)
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
            // Only process if reminder is ACTIVE
            if (reminder.status != ReminderStatus.ACTIVE) return@runBlocking

            val now = System.currentTimeMillis()
            var updated = reminder

            if (isSnoozeFire) {
                updated = reminder.copy(snoozedUntil = null, updatedAt = now)
                app.database.reminderDao().update(updated)
            }

            // Schedule next notification in 30 minutes if reminder is still ACTIVE
            val thirtyMinutesFromNow = now + (30 * 60 * 1000L)
            app.reminderScheduler.scheduleReminder(
                reminderId = reminderId,
                triggerAtMillis = thirtyMinutesFromNow,
                title = updated.title,
                notes = updated.notes.ifEmpty() { "Reminder" },
                isSnoozeFire = false,
                requestCodeOffset = ReminderScheduler.THIRTY_MIN_REMINDER_OFFSET
            )

            // For recurring reminders, also schedule the next occurrence
            if (updated.type != RecurrenceType.NONE) {
                val nextDue = nextOccurrenceMillis(updated.dueAt, updated.type) ?: return@runBlocking
                val nextEntity = updated.copy(dueAt = nextDue, updatedAt = now)
                app.database.reminderDao().update(nextEntity)
                // Schedule the next occurrence (this will be in addition to the 30-minute reminder)
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

    private fun buildSnoozeActions(context: Context, reminderId: Long, title: String, notes: String): List<Pair<String, PendingIntent>> {
        val actions = mutableListOf<Pair<String, PendingIntent>>()
        val appContext = context.applicationContext
        val enabledOptions = AppPreferences(appContext).getEnabledSnoozeOptions()
        
        // Ensure at least one option is enabled (fallback to default if empty)
        val optionsToShow = if (enabledOptions.isEmpty()) {
            setOf("15_min", "1_hour", "personalizar")
        } else {
            enabledOptions
        }
        
        var requestCodeOffset = 0
        
        // "15 minutos" action (if enabled)
        if (optionsToShow.contains("15_min")) {
            val snooze15Intent = Intent(appContext, SnoozeReceiver::class.java).apply {
                action = SnoozeReceiver.ACTION_SNOOZE_REMINDER
                putExtra(SnoozeReceiver.EXTRA_REMINDER_ID, reminderId)
                putExtra(SnoozeReceiver.EXTRA_TITLE, title)
                putExtra(SnoozeReceiver.EXTRA_NOTES, notes)
                putExtra(SnoozeReceiver.EXTRA_SNOOZE_DURATION_MS, 15 * 60 * 1000L)
            }
            val snooze15Pending = PendingIntent.getBroadcast(
                appContext,
                reminderId.toInt() + SNOOZE_REQUEST_CODE_OFFSET + requestCodeOffset,
                snooze15Intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(appContext.getString(R.string.snooze_15_min) to snooze15Pending)
            requestCodeOffset++
        }
        
        // "1 hora" action (if enabled)
        if (optionsToShow.contains("1_hour")) {
            val snooze1HourIntent = Intent(appContext, SnoozeReceiver::class.java).apply {
                action = SnoozeReceiver.ACTION_SNOOZE_REMINDER
                putExtra(SnoozeReceiver.EXTRA_REMINDER_ID, reminderId)
                putExtra(SnoozeReceiver.EXTRA_TITLE, title)
                putExtra(SnoozeReceiver.EXTRA_NOTES, notes)
                putExtra(SnoozeReceiver.EXTRA_SNOOZE_DURATION_MS, 60 * 60 * 1000L)
            }
            val snooze1HourPending = PendingIntent.getBroadcast(
                appContext,
                reminderId.toInt() + SNOOZE_REQUEST_CODE_OFFSET + requestCodeOffset,
                snooze1HourIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(appContext.getString(R.string.snooze_1_hour) to snooze1HourPending)
            requestCodeOffset++
        }
        
        // "Personalizar" action (if enabled)
        if (optionsToShow.contains("personalizar")) {
            val personalizarIntent = Intent(appContext, SnoozeReceiver::class.java).apply {
                action = SnoozeReceiver.ACTION_SNOOZE_REMINDER
                putExtra(SnoozeReceiver.EXTRA_REMINDER_ID, reminderId)
                putExtra(SnoozeReceiver.EXTRA_TITLE, title)
                putExtra(SnoozeReceiver.EXTRA_NOTES, notes)
                putExtra(SnoozeReceiver.EXTRA_IS_CUSTOM, true)
            }
            val personalizarPending = PendingIntent.getBroadcast(
                appContext,
                reminderId.toInt() + SNOOZE_REQUEST_CODE_OFFSET + requestCodeOffset,
                personalizarIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(appContext.getString(R.string.snooze_personalizar) to personalizarPending)
        }
        
        return actions
    }
    
    private fun showCompletionCheckNotification(context: Context, reminderId: Long, title: String, notes: String) {
        val appContext = context.applicationContext
        val message = appContext.getString(R.string.fazendo_followup_message, title)
        
        val actions = mutableListOf<Pair<String, PendingIntent>>()
        
        // "Sim" action (mark as complete)
        val completeIntent = Intent(appContext, CompletionCheckReceiver::class.java).apply {
            action = CompletionCheckReceiver.ACTION_COMPLETE_REMINDER
            putExtra(CompletionCheckReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val completePending = PendingIntent.getBroadcast(
            appContext,
            reminderId.toInt() + COMPLETION_CHECK_REQUEST_CODE_OFFSET,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        actions.add(appContext.getString(R.string.fazendo_complete) to completePending)
        
        // "+15 min" action
        val snooze15Intent = Intent(appContext, CompletionCheckReceiver::class.java).apply {
            action = CompletionCheckReceiver.ACTION_SNOOZE_REMINDER
            putExtra(CompletionCheckReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(CompletionCheckReceiver.EXTRA_TITLE, title)
            putExtra(CompletionCheckReceiver.EXTRA_NOTES, notes)
            putExtra(CompletionCheckReceiver.EXTRA_SNOOZE_DURATION_MS, 15 * 60 * 1000L)
        }
        val snooze15Pending = PendingIntent.getBroadcast(
            appContext,
            reminderId.toInt() + COMPLETION_CHECK_REQUEST_CODE_OFFSET + 1,
            snooze15Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        actions.add(appContext.getString(R.string.fazendo_snooze_15_min) to snooze15Pending)
        
        // "+1 hora" action
        val snooze1HourIntent = Intent(appContext, CompletionCheckReceiver::class.java).apply {
            action = CompletionCheckReceiver.ACTION_SNOOZE_REMINDER
            putExtra(CompletionCheckReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(CompletionCheckReceiver.EXTRA_TITLE, title)
            putExtra(CompletionCheckReceiver.EXTRA_NOTES, notes)
            putExtra(CompletionCheckReceiver.EXTRA_SNOOZE_DURATION_MS, 60 * 60 * 1000L)
        }
        val snooze1HourPending = PendingIntent.getBroadcast(
            appContext,
            reminderId.toInt() + COMPLETION_CHECK_REQUEST_CODE_OFFSET + 2,
            snooze1HourIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        actions.add(appContext.getString(R.string.fazendo_snooze_1_hour) to snooze1HourPending)
        
        // "Personalizar" action
        val personalizarIntent = Intent(appContext, CompletionCheckReceiver::class.java).apply {
            action = CompletionCheckReceiver.ACTION_SNOOZE_REMINDER
            putExtra(CompletionCheckReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(CompletionCheckReceiver.EXTRA_TITLE, title)
            putExtra(CompletionCheckReceiver.EXTRA_NOTES, notes)
            putExtra(CompletionCheckReceiver.EXTRA_IS_CUSTOM, true)
        }
        val personalizarPending = PendingIntent.getBroadcast(
            appContext,
            reminderId.toInt() + COMPLETION_CHECK_REQUEST_CODE_OFFSET + 3,
            personalizarIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        actions.add(appContext.getString(R.string.fazendo_snooze_personalizar) to personalizarPending)
        
        NotificationHelper.showCompletionCheckNotification(
            appContext,
            reminderId,
            message,
            actions
        )
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_NOTES = "notes"
        const val EXTRA_IS_SNOOZE_FIRE = "is_snooze_fire"
        const val EXTRA_IS_FAZENDO_FOLLOWUP = "is_fazendo_followup"
        
        private const val COMPLETION_CHECK_REQUEST_CODE_OFFSET = 3_000_000
    }
}
