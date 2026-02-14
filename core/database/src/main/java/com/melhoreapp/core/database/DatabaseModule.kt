package com.melhoreapp.core.database

import android.content.Context
import androidx.room.Room
import com.melhoreapp.core.database.dao.CategoryDao
import com.melhoreapp.core.database.dao.ListDao
import com.melhoreapp.core.database.dao.ReminderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMelhoreDatabase(
        @ApplicationContext context: Context
    ): MelhoreDatabase = Room.databaseBuilder(
        context,
        MelhoreDatabase::class.java,
        "melhore_db"
    ).build()

    @Provides
    @Singleton
    fun provideReminderDao(database: MelhoreDatabase): ReminderDao = database.reminderDao()

    @Provides
    @Singleton
    fun provideCategoryDao(database: MelhoreDatabase): CategoryDao = database.categoryDao()

    @Provides
    @Singleton
    fun provideListDao(database: MelhoreDatabase): ListDao = database.listDao()
}
