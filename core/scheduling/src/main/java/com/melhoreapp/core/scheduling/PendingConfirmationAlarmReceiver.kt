package com.melhoreapp.core.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Receives alarms scheduled per one-time reminder at dueAt + 60 minutes. When fired,
 * enqueues a one-time [PendingConfirmationCheckWorker] to query all pending-confirmation
 * reminders and show the "Melhores pendentes de confirmação" notification if any.
 * This makes the notification fire based on each Melhore's due date, not app open or hourly tick.
 */
class PendingConfirmationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_PENDING_CONFIRMATION_CHECK) return
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        Log.d(TAG, "Pending confirmation alarm fired for reminderId=$reminderId, enqueuing one-time check")
        val request = OneTimeWorkRequestBuilder<PendingConfirmationCheckWorker>()
            .setInitialDelay(0, TimeUnit.MILLISECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    companion object {
        private const val TAG = "PendingConfirmationAlarm"
        const val ACTION_PENDING_CONFIRMATION_CHECK = "com.melhoreapp.core.scheduling.PENDING_CONFIRMATION_CHECK"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
