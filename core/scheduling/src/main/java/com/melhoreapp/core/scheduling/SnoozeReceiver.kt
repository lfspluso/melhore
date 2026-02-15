package com.melhoreapp.core.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.melhoreapp.core.common.preferences.AppPreferences
import kotlinx.coroutines.runBlocking

/** Default snooze when no duration extra is provided (10 minutes). Kept for backward compatibility. */
const val SNOOZE_DEFAULT_MS = 10 * 60 * 1000L

/** Request code offset for snooze PendingIntents so they do not collide with main alarm PendingIntents. */
const val SNOOZE_REQUEST_CODE_OFFSET = 1_000_000

/** Snooze preset durations in milliseconds: 5 min, 15 min, 1 hour, 1 day. */
val SNOOZE_DURATIONS_MS: List<Long> = listOf(
    5 * 60 * 1000L,
    15 * 60 * 1000L,
    60 * 60 * 1000L,
    24 * 60 * 60 * 1000L
)

/** Number of snooze preset actions (used for cancel when deleting a reminder). */
const val SNOOZE_PRESET_COUNT = 4

/**
 * Receives snooze action from reminder notification. Duration is read from EXTRA_SNOOZE_DURATION_MS
 * (user chooses minutes, hours, or days via different notification actions). Sets snoozedUntil, updates DB,
 * and schedules one alarm at snoozedUntil with isSnoozeFire=true.
 */
class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SNOOZE_REMINDER) return
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId < 0) return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
        val notes = intent.getStringExtra(EXTRA_NOTES) ?: "Reminder"
        val rawDuration = intent.getLongExtra(EXTRA_SNOOZE_DURATION_MS, -1L)
        val durationMs = (if (rawDuration >= 0) rawDuration
            else AppPreferences(context.applicationContext).getDefaultSnoozeDurationMs())
            .coerceIn(MIN_SNOOZE_MS, MAX_SNOOZE_MS)

        val app = context.applicationContext as? SchedulingContext ?: return
        runBlocking {
            val reminder = app.database.reminderDao().getReminderById(reminderId) ?: return@runBlocking
            if (!reminder.isActive) return@runBlocking

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

        // Dismiss the current notification so the snoozed one appears at snoozedUntil
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.cancel(reminderId.toInt())
    }

    companion object {
        const val ACTION_SNOOZE_REMINDER = "com.melhoreapp.core.scheduling.SNOOZE_REMINDER"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_NOTES = "notes"
        const val EXTRA_SNOOZE_DURATION_MS = "snooze_duration_ms"

        private const val MIN_SNOOZE_MS = 60 * 1000L       // 1 minute
        private const val MAX_SNOOZE_MS = 30 * 24 * 60 * 60 * 1000L  // 30 days
    }
}
