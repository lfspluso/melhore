package com.melhoreapp.core.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.melhoreapp.core.common.preferences.AppPreferences
import com.melhoreapp.core.database.MelhoreDatabase
import com.melhoreapp.core.database.entity.ReminderEntity
import com.melhoreapp.core.database.entity.RecurrenceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Schedules and cancels one-time reminder notifications via AlarmManager (exact alarm).
 * Notifications fire at due time even when the app is killed. Used when a reminder is
 * created (schedule), deleted (cancel), or after device boot (reschedule all upcoming).
 * Scheduling never throws so the UI can always close after save; failures are logged.
 */
class ReminderScheduler(
    private val context: Context,
    private val alarmManager: AlarmManager,
    private val database: MelhoreDatabase,
    private val appPreferences: AppPreferences
) {

    fun scheduleReminder(
        reminderId: Long,
        triggerAtMillis: Long,
        title: String,
        notes: String,
        isSnoozeFire: Boolean = false,
        requestCodeOffset: Int = 0,
        isFazendoFollowup: Boolean = false,
        isTaskCheckup: Boolean = false
    ) {
        try {
            val triggerTime = triggerAtMillis.coerceAtLeast(System.currentTimeMillis() + MIN_FUTURE_MS)
            val intent = alarmIntent(reminderId, title, notes, isSnoozeFire, isFazendoFollowup, isTaskCheckup)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId.toInt() + requestCodeOffset,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val showIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
                    PendingIntent.getActivity(
                        context,
                        0,
                        it,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerTime, showIntent),
                    pendingIntent
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm not allowed (grant SCHEDULE_EXACT_ALARM in settings): reminderId=$reminderId", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule reminder alarm: reminderId=$reminderId", e)
        }
    }

    /**
     * Returns true if the app can schedule exact alarms (required for reminders to fire on time).
     * On API 31+, the user may need to grant "Alarms & reminders" in settings.
     */
    fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true

    fun cancelReminder(reminderId: Long) {
        val mainIntent = alarmIntent(reminderId, "", "", isSnoozeFire = false)
        val mainPending = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(mainPending)
        // Cancel 30-minute reminder
        val thirtyMinIntent = alarmIntent(reminderId, "", "", isSnoozeFire = false)
        val thirtyMinPending = PendingIntent.getBroadcast(
            context,
            reminderId.toInt() + THIRTY_MIN_REMINDER_OFFSET,
            thirtyMinIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(thirtyMinPending)
        for (index in 0 until SNOOZE_PRESET_COUNT) {
            val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
                action = SnoozeReceiver.ACTION_SNOOZE_REMINDER
            }
            val snoozePending = PendingIntent.getBroadcast(
                context,
                reminderId.toInt() + SNOOZE_REQUEST_CODE_OFFSET + index,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(snoozePending)
        }
    }

    /**
     * Reschedules all active reminders with due time in the future. Call after BOOT_COMPLETED
     * since alarms are cleared on device reboot.
     */
    suspend fun rescheduleAllUpcomingReminders() = withContext(Dispatchers.IO) {
        val userId = appPreferences.getLastUserId() ?: "local"
        val now = System.currentTimeMillis()
        val active = database.reminderDao().getActiveReminders(userId)
        active.forEach { reminder ->
            val triggerAt = computeTriggerTime(reminder)
            if (triggerAt != null) {
                val snoozedUntil = reminder.snoozedUntil
                val isSnoozeFire = snoozedUntil != null && snoozedUntil > now && triggerAt == snoozedUntil
                scheduleReminder(
                    reminderId = reminder.id,
                    triggerAtMillis = triggerAt,
                    title = reminder.title,
                    notes = reminder.notes.ifEmpty() { "Reminder" },
                    isSnoozeFire = isSnoozeFire
                )
            }
        }
    }

    /**
     * Computes trigger time for a reminder: snoozedUntil if set and in future, else dueAt.
     * For recurring with trigger in the past, advances dueAt to next occurrence and updates DB;
     * returns the new trigger time.
     */
    private suspend fun computeTriggerTime(reminder: ReminderEntity): Long? {
        val now = System.currentTimeMillis()
        val snoozedUntil = reminder.snoozedUntil
        var triggerAt = if (snoozedUntil != null && snoozedUntil > now) {
            snoozedUntil
        } else {
            reminder.dueAt
        }
        if (triggerAt <= now && reminder.type != RecurrenceType.NONE) {
            val next = nextOccurrenceMillis(reminder.dueAt, reminder.type, reminder.customRecurrenceDays) ?: return null
            database.reminderDao().update(reminder.copy(dueAt = next, updatedAt = now))
            triggerAt = next
        } else if (triggerAt <= now) {
            return null
        }
        return triggerAt
    }

    private fun alarmIntent(reminderId: Long, title: String, notes: String, isSnoozeFire: Boolean, isFazendoFollowup: Boolean = false, isTaskCheckup: Boolean = false): Intent {
        return Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderAlarmReceiver.EXTRA_TITLE, title)
            putExtra(ReminderAlarmReceiver.EXTRA_NOTES, notes.ifEmpty() { "Reminder" })
            putExtra(ReminderAlarmReceiver.EXTRA_IS_SNOOZE_FIRE, isSnoozeFire)
            putExtra(ReminderAlarmReceiver.EXTRA_IS_FAZENDO_FOLLOWUP, isFazendoFollowup)
            putExtra(ReminderAlarmReceiver.EXTRA_IS_TASK_CHECKUP, isTaskCheckup)
        }
    }

    companion object {
        // Offset for 30-minute recurring reminders (different from main reminder alarm)
        const val THIRTY_MIN_REMINDER_OFFSET = 2_000_000
        
        private const val MIN_FUTURE_MS = 1000L
        private const val TAG = "ReminderScheduler"
    }
}
