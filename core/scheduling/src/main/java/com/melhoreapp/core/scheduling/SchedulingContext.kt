package com.melhoreapp.core.scheduling

import com.melhoreapp.core.database.MelhoreDatabase

/**
 * Provides scheduling and database access for components that cannot use Hilt
 * (e.g. BroadcastReceivers). The Application implements this and is cast in receivers.
 */
interface SchedulingContext {
    val database: MelhoreDatabase
    val reminderScheduler: ReminderScheduler
}
