package com.melhoreapp.core.scheduling

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.melhoreapp.core.database.MelhoreDatabase
import com.melhoreapp.core.notifications.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker that runs at reminder time and shows a notification.
 * Scheduled by ReminderScheduler when a reminder is created.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val database: MelhoreDatabase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getLong(KEY_REMINDER_ID, -1L)
        if (reminderId < 0) return Result.failure()
        val reminder = database.reminderDao().getReminderById(reminderId) ?: return Result.failure()
        if (!reminder.isActive) return Result.success()
        NotificationHelper.showReminderNotification(
            applicationContext,
            reminder.id,
            reminder.title,
            reminder.notes.ifEmpty() { "Reminder" }
        )
        return Result.success()
    }

    companion object {
        const val KEY_REMINDER_ID = "reminder_id"
    }
}
