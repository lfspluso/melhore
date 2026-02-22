package com.melhoreapp.core.scheduling

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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

        val app = context.applicationContext as? SchedulingContext ?: return
        try {
            runBlocking {
                val reminder = app.database.reminderDao().getReminderById(reminderId) ?: return@runBlocking
                // Only process if reminder is ACTIVE
                if (reminder.status != ReminderStatus.ACTIVE) return@runBlocking

                // Check if this is a Rotina reminder and if tasks already exist for current period
            if (reminder.isRoutine && !isSnoozeFire) {
                val userId = reminder.userId ?: "local"
                val tasks = app.database.reminderDao().getTasksByParentReminderIdOnce(userId, reminderId)
                val periodStart = RotinaPeriodHelper.getCurrentPeriodStart(reminder)
                val periodEnd = RotinaPeriodHelper.getCurrentPeriodEnd(reminder)
                
                // Check if any tasks exist within the current period
                val hasTasksInPeriod = tasks.any { task ->
                    task.startTime != null && task.startTime!! in periodStart..periodEnd
                }
                
                if (hasTasksInPeriod) {
                    // Tasks already exist for this period, skip notification and advance to next occurrence
                    val now = System.currentTimeMillis()
                    val nextDue = nextOccurrenceMillis(reminder.dueAt, reminder.type, reminder.customRecurrenceDays) ?: return@runBlocking
                    val nextEntity = reminder.copy(dueAt = nextDue, updatedAt = now)
                    app.database.reminderDao().update(nextEntity)
                    
                    // Schedule the next occurrence
                    app.reminderScheduler.scheduleReminder(
                        reminderId = reminderId,
                        triggerAtMillis = nextDue,
                        title = nextEntity.title,
                        notes = nextEntity.notes.ifEmpty() { "Reminder" },
                        isSnoozeFire = false
                    )
                    return@runBlocking
                }
            }

            // Check if this is a task reminder
            val isTaskCheckup = intent.getBooleanExtra(EXTRA_IS_TASK_CHECKUP, false)
            if (reminder.isTask && isTaskCheckup) {
                // Handle task checkup notification (delegated to TaskCheckupReceiver logic)
                handleTaskCheckupNotification(context, reminder, title, notes)
                return@runBlocking
            }

            // Check if this is initial task notification
            if (reminder.isTask && reminder.startTime != null && reminder.startTime == reminder.dueAt) {
                // Initial task notification - show notification and schedule first checkup
                val snoozeActions = buildSnoozeActions(appContext, reminderId, title, notes)
                NotificationHelper.showReminderNotification(
                    appContext,
                    reminderId,
                    title,
                    notes,
                    snoozeActions,
                    isRoutine = false
                )
                // Schedule first checkup if checkupFrequencyHours is set
                if (reminder.checkupFrequencyHours != null && reminder.checkupFrequencyHours!! > 0) {
                    val firstCheckupTime = reminder.startTime!! + (reminder.checkupFrequencyHours!! * 60 * 60 * 1000L)
                    app.reminderScheduler.scheduleReminder(
                        reminderId = reminderId,
                        triggerAtMillis = firstCheckupTime,
                        title = reminder.title,
                        notes = reminder.notes.ifEmpty() { "Reminder" },
                        isSnoozeFire = false,
                        isTaskCheckup = true
                    )
                }
                return@runBlocking
            }
        
            // Regular reminder notification with new snooze options
            val snoozeActions = buildSnoozeActions(appContext, reminderId, title, notes, reminder.isRoutine)
            NotificationHelper.showReminderNotification(
                appContext,
                reminderId,
                title,
                notes,
                snoozeActions,
                isRoutine = reminder.isRoutine
            )

            val now = System.currentTimeMillis()
            var updated = reminder

            if (isSnoozeFire) {
                updated = reminder.copy(snoozedUntil = null, updatedAt = now)
                app.database.reminderDao().update(updated)
            }

            // Schedule next notification in 30 minutes if reminder is still ACTIVE (Rotinas too: until user skips day or adds tasks)
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
                val nextDue = nextOccurrenceMillis(updated.dueAt, updated.type, updated.customRecurrenceDays) ?: return@runBlocking
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
        } catch (e: Exception) {
            Log.e(TAG, "ReminderAlarmReceiver failed for reminderId=$reminderId", e)
        }
    }

    private fun handleTaskCheckupNotification(context: Context, reminder: com.melhoreapp.core.database.entity.ReminderEntity, title: String, notes: String) {
        val appContext = context.applicationContext
        val actions = mutableListOf<Pair<String, PendingIntent>>()
        
        // "Done" action
        val doneIntent = Intent(appContext, TaskCheckupReceiver::class.java).apply {
            action = TaskCheckupReceiver.ACTION_DONE_TASK
            putExtra(TaskCheckupReceiver.EXTRA_REMINDER_ID, reminder.id)
        }
        val donePending = PendingIntent.getBroadcast(
            appContext,
            reminder.id.toInt() + TASK_CHECKUP_REQUEST_CODE_OFFSET,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        actions.add(appContext.getString(com.melhoreapp.core.notifications.R.string.task_checkup_done) to donePending)
        
        // "Snooze" action
        val snoozeIntent = Intent(appContext, SnoozeReceiver::class.java).apply {
            action = SnoozeReceiver.ACTION_SNOOZE_REMINDER
            putExtra(SnoozeReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(SnoozeReceiver.EXTRA_TITLE, title)
            putExtra(SnoozeReceiver.EXTRA_NOTES, notes)
            putExtra(SnoozeReceiver.EXTRA_SNOOZE_DURATION_MS, 15 * 60 * 1000L) // Default 15 min
        }
        val snoozePending = PendingIntent.getBroadcast(
            appContext,
            reminder.id.toInt() + TASK_CHECKUP_REQUEST_CODE_OFFSET + 1,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        actions.add(appContext.getString(com.melhoreapp.core.notifications.R.string.task_checkup_snooze) to snoozePending)
        
        // "Continue" action
        val continueIntent = Intent(appContext, TaskCheckupReceiver::class.java).apply {
            action = TaskCheckupReceiver.ACTION_CONTINUE_TASK
            putExtra(TaskCheckupReceiver.EXTRA_REMINDER_ID, reminder.id)
        }
        val continuePending = PendingIntent.getBroadcast(
            appContext,
            reminder.id.toInt() + TASK_CHECKUP_REQUEST_CODE_OFFSET + 2,
            continueIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        actions.add(appContext.getString(com.melhoreapp.core.notifications.R.string.task_checkup_continue) to continuePending)
        
        NotificationHelper.showReminderNotification(
            appContext,
            reminder.id,
            title,
            notes,
            actions,
            isRoutine = false
        )
    }

    private fun buildSnoozeActions(context: Context, reminderId: Long, title: String, notes: String, isRoutine: Boolean = false): List<Pair<String, PendingIntent>> {
        val actions = mutableListOf<Pair<String, PendingIntent>>()
        val appContext = context.applicationContext
        val enabledOptions = AppPreferences(appContext).getEnabledSnoozeOptions()
        
        // Ensure at least one option is enabled (fallback to default if empty)
        val optionsToShow = if (enabledOptions.isEmpty()) {
            setOf("5_min", "15_min", "1_hour")
        } else {
            enabledOptions
        }
        
        var requestCodeOffset = 0
        
        // "5 minutos" action (if enabled)
        if (optionsToShow.contains("5_min")) {
            val snooze5Intent = Intent(appContext, SnoozeReceiver::class.java).apply {
                action = SnoozeReceiver.ACTION_SNOOZE_REMINDER
                putExtra(SnoozeReceiver.EXTRA_REMINDER_ID, reminderId)
                putExtra(SnoozeReceiver.EXTRA_TITLE, title)
                putExtra(SnoozeReceiver.EXTRA_NOTES, notes)
                putExtra(SnoozeReceiver.EXTRA_SNOOZE_DURATION_MS, 5 * 60 * 1000L)
            }
            val snooze5Pending = PendingIntent.getBroadcast(
                appContext,
                reminderId.toInt() + SNOOZE_REQUEST_CODE_OFFSET + requestCodeOffset,
                snooze5Intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(appContext.getString(R.string.snooze_5_min) to snooze5Pending)
            requestCodeOffset++
        }
        
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
        
        // "30 minutos" action (if enabled)
        if (optionsToShow.contains("30_min")) {
            val snooze30Intent = Intent(appContext, SnoozeReceiver::class.java).apply {
                action = SnoozeReceiver.ACTION_SNOOZE_REMINDER
                putExtra(SnoozeReceiver.EXTRA_REMINDER_ID, reminderId)
                putExtra(SnoozeReceiver.EXTRA_TITLE, title)
                putExtra(SnoozeReceiver.EXTRA_NOTES, notes)
                putExtra(SnoozeReceiver.EXTRA_SNOOZE_DURATION_MS, 30 * 60 * 1000L)
            }
            val snooze30Pending = PendingIntent.getBroadcast(
                appContext,
                reminderId.toInt() + SNOOZE_REQUEST_CODE_OFFSET + requestCodeOffset,
                snooze30Intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(appContext.getString(R.string.snooze_30_min) to snooze30Pending)
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
        
        // "2 horas" action (if enabled)
        if (optionsToShow.contains("2_hours")) {
            val snooze2HoursIntent = Intent(appContext, SnoozeReceiver::class.java).apply {
                action = SnoozeReceiver.ACTION_SNOOZE_REMINDER
                putExtra(SnoozeReceiver.EXTRA_REMINDER_ID, reminderId)
                putExtra(SnoozeReceiver.EXTRA_TITLE, title)
                putExtra(SnoozeReceiver.EXTRA_NOTES, notes)
                putExtra(SnoozeReceiver.EXTRA_SNOOZE_DURATION_MS, 2 * 60 * 60 * 1000L)
            }
            val snooze2HoursPending = PendingIntent.getBroadcast(
                appContext,
                reminderId.toInt() + SNOOZE_REQUEST_CODE_OFFSET + requestCodeOffset,
                snooze2HoursIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(appContext.getString(R.string.snooze_2_hours) to snooze2HoursPending)
            requestCodeOffset++
        }
        
        // "1 dia" action (if enabled)
        if (optionsToShow.contains("1_day")) {
            val snooze1DayIntent = Intent(appContext, SnoozeReceiver::class.java).apply {
                action = SnoozeReceiver.ACTION_SNOOZE_REMINDER
                putExtra(SnoozeReceiver.EXTRA_REMINDER_ID, reminderId)
                putExtra(SnoozeReceiver.EXTRA_TITLE, title)
                putExtra(SnoozeReceiver.EXTRA_NOTES, notes)
                putExtra(SnoozeReceiver.EXTRA_SNOOZE_DURATION_MS, 24 * 60 * 60 * 1000L)
            }
            val snooze1DayPending = PendingIntent.getBroadcast(
                appContext,
                reminderId.toInt() + SNOOZE_REQUEST_CODE_OFFSET + requestCodeOffset,
                snooze1DayIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(appContext.getString(R.string.snooze_1_day) to snooze1DayPending)
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
        
        // Add "Skip day" action for Rotina reminders
        if (isRoutine) {
            val skipDayAction = buildSkipDayAction(appContext, reminderId)
            actions.add(skipDayAction)
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

    private fun buildSkipDayAction(context: Context, reminderId: Long): Pair<String, PendingIntent> {
        val appContext = context.applicationContext
        val skipIntent = Intent(appContext, RoutineSkipReceiver::class.java).apply {
            action = RoutineSkipReceiver.ACTION_SKIP_ROUTINE_DAY
            putExtra(RoutineSkipReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        val skipPending = PendingIntent.getBroadcast(
            appContext,
            reminderId.toInt() + ROUTINE_SKIP_REQUEST_CODE_OFFSET,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return appContext.getString(com.melhoreapp.core.notifications.R.string.routine_skip_day) to skipPending
    }

    companion object {
        private const val TAG = "ReminderAlarmReceiver"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_NOTES = "notes"
        const val EXTRA_IS_SNOOZE_FIRE = "is_snooze_fire"
        const val EXTRA_IS_FAZENDO_FOLLOWUP = "is_fazendo_followup"
        const val EXTRA_IS_TASK_CHECKUP = "is_task_checkup"
        
        private const val COMPLETION_CHECK_REQUEST_CODE_OFFSET = 3_000_000
        private const val ROUTINE_SKIP_REQUEST_CODE_OFFSET = 4_000_000
        private const val TASK_CHECKUP_REQUEST_CODE_OFFSET = 5_000_000
    }
}
