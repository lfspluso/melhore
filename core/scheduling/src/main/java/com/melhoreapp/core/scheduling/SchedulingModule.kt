package com.melhoreapp.core.scheduling

import android.app.AlarmManager
import android.content.Context
import androidx.work.WorkManager
import com.melhoreapp.core.common.preferences.AppPreferences
import com.melhoreapp.core.database.MelhoreDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SchedulingModule {

    @Provides
    @Singleton
    fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @Provides
    @Singleton
    fun provideReminderScheduler(
        @ApplicationContext context: Context,
        alarmManager: AlarmManager,
        database: MelhoreDatabase,
        appPreferences: AppPreferences
    ): ReminderScheduler = ReminderScheduler(context, alarmManager, database, appPreferences)

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences =
        AppPreferences(context)
}
