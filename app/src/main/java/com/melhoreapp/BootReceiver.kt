package com.melhoreapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.runBlocking

/**
 * Reschedules all upcoming reminder alarms after device reboot.
 * Alarms are cleared on reboot; this receiver runs on BOOT_COMPLETED and restores them.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? MelhoreApplication ?: return
        try {
            runBlocking {
                app.reminderScheduler.rescheduleAllUpcomingReminders()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reschedule reminders after boot", e)
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
    }
}
