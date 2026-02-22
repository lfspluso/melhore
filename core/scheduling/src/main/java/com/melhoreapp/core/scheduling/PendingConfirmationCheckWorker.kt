package com.melhoreapp.core.scheduling

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.melhoreapp.core.auth.AuthRepository
import com.melhoreapp.core.database.MelhoreDatabase
import com.melhoreapp.core.database.entity.RecurrenceType
import com.melhoreapp.core.database.entity.ReminderStatus
import com.melhoreapp.core.database.entity.ReminderEntity
import com.melhoreapp.core.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Periodic worker that checks every hour for reminders with PENDENTE CONFIRMAÇÃO status
 * and notifies the user if any are found.
 * Includes both one-time Melhores and Tarefas created by Rotinas (they have isRoutine=false,
 * type=NONE; Rotina parents are excluded by isRoutine).
 */
@HiltWorker
class PendingConfirmationCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val database: MelhoreDatabase,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()
            Log.d(TAG, "PendingConfirmationCheckWorker doWork started now=$now")

            // Get current user ID (or "local" if not signed in)
            val userId = authRepository.currentUser.first()?.userId ?: "local"
            Log.d(TAG, "userId=$userId")

            // Get all active reminders for current user (includes task reminders from Rotinas)
            val activeReminders = database.reminderDao().getActiveReminders(userId)
            Log.d(TAG, "activeReminders count=${activeReminders.size}")

            // Filter for pending confirmation reminders and log why each is included or excluded.
            // Rotinas (isRoutine) are never pending confirmation: they recur and notify at next occurrence or user skips day.
            // Task reminders created by Rotinas (isRoutine=false, type=NONE, parentReminderId!=null) are included.
            val pendingReminders = mutableListOf<ReminderEntity>()
            for (reminder in activeReminders) {
                if (reminder.isRoutine) {
                    Log.d(TAG, "reminder id=${reminder.id} title=\"${reminder.title}\" excluded: isRoutine (Rotinas are not pending confirmation)")
                    continue
                }
                val passesActive = reminder.status == ReminderStatus.ACTIVE
                val passesDue = reminder.dueAt <= now
                val passesSnooze = reminder.snoozedUntil == null || reminder.snoozedUntil!! <= now
                val passesType = reminder.type == RecurrenceType.NONE
                val passes = passesActive && passesDue && passesSnooze && passesType
                val reasons = mutableListOf<String>()
                if (!passesActive) reasons.add("status!=ACTIVE(${reminder.status})")
                if (!passesDue) reasons.add("dueAt(${reminder.dueAt})>now($now)")
                if (!passesSnooze) reasons.add("snoozedUntil=${reminder.snoozedUntil}")
                if (!passesType) reasons.add("type!=NONE(${reminder.type})")
                Log.d(TAG, "reminder id=${reminder.id} title=\"${reminder.title}\" dueAt=${reminder.dueAt} status=${reminder.status} type=${reminder.type} snoozedUntil=${reminder.snoozedUntil} passes=$passes ${if (reasons.isNotEmpty()) "excluded: ${reasons.joinToString()}" else ""}")
                if (passes) pendingReminders.add(reminder)
            }

            Log.d(TAG, "pendingReminders count=${pendingReminders.size} ids=${pendingReminders.map { it.id }} titles=${pendingReminders.map { it.title }}")
            if (pendingReminders.isNotEmpty()) {
                Log.d(TAG, "Showing pending confirmation notification with ${pendingReminders.size} reminder(s)")
                showPendingConfirmationNotification(applicationContext, pendingReminders)
            } else {
                Log.d(TAG, "No pending reminders, notification not shown")
            }

            Log.d(TAG, "PendingConfirmationCheckWorker doWork finished")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "PendingConfirmationCheckWorker doWork failed", e)
            Result.failure()
        }
    }

    private fun showPendingConfirmationNotification(
        context: Context,
        reminders: List<com.melhoreapp.core.database.entity.ReminderEntity>
    ) {
        val title = "Melhores pendentes de confirmação"
        val reminderTitles = reminders.take(5).joinToString("\n") { it.title }
        val text = if (reminders.size > 5) {
            "$reminderTitles\n... e mais ${reminders.size - 5}"
        } else {
            reminderTitles
        }
        
        // Create intent to open app to Tarefas tab
        val contentIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.let { intent ->
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.putExtra("navigation_route", "reminders?tab=tarefas")
                PendingIntent.getActivity(
                    context,
                    PENDING_CONFIRMATION_NOTIFICATION_ID,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        
        val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        
        contentIntent?.let { builder.setContentIntent(it) }
        
        NotificationManagerCompat.from(context).notify(
            PENDING_CONFIRMATION_NOTIFICATION_ID,
            builder.build()
        )
    }

    companion object {
        private const val TAG = "PendingConfirmation"
        const val PENDING_CONFIRMATION_NOTIFICATION_ID = 999999
        const val WORK_NAME = "pending_confirmation_check"
        
        /**
         * Schedules periodic work to check for pending confirmation reminders at the top of each hour
         * (e.g. 18:00, 19:00), so reminders due at e.g. 17:55 are picked up at 18:00. The first run
         * is delayed until the next :00; subsequent runs occur every hour.
         */
        fun schedule(context: Context) {
            val now = ZonedDateTime.now()
            val nextTopOfHour = now.truncatedTo(ChronoUnit.HOURS).plusHours(1)
            var delayMillis = nextTopOfHour.toInstant().toEpochMilli() - System.currentTimeMillis()
            if (delayMillis < 0) delayMillis = 0L
            Log.d(TAG, "PendingConfirmationCheckWorker first run at top of next hour (delay=${delayMillis}ms)")
            val workRequest = PeriodicWorkRequestBuilder<PendingConfirmationCheckWorker>(
                1, TimeUnit.HOURS
            )
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
