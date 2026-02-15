package com.melhoreapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.melhoreapp.core.database.MelhoreDatabase
import com.melhoreapp.core.notifications.NotificationHelper
import com.melhoreapp.core.scheduling.ReminderScheduler
import com.melhoreapp.core.scheduling.SchedulingContext
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MelhoreApplication : Application(), Configuration.Provider, SchedulingContext {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    override lateinit var reminderScheduler: ReminderScheduler

    @Inject
    override lateinit var database: MelhoreDatabase

    override fun onCreate() {
        super.onCreate()
        // Initialize WorkManager with HiltWorkerFactory so ReminderWorker (HiltWorker) can be created.
        WorkManager.initialize(this, workManagerConfiguration)
        NotificationHelper.createChannels(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
