package com.melhoreapp.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.melhoreapp.core.database.dao.CategoryDao
import com.melhoreapp.core.database.dao.ChecklistItemDao
import com.melhoreapp.core.database.dao.ListDao
import com.melhoreapp.core.database.dao.ReminderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS checklist_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                reminderId INTEGER NOT NULL,
                label TEXT NOT NULL,
                sortOrder INTEGER NOT NULL DEFAULT 0,
                checked INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(reminderId) REFERENCES reminders(id) ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_checklist_items_reminderId ON checklist_items(reminderId)")
    }
}

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
    ).addMigrations(MIGRATION_1_2).build()

    @Provides
    @Singleton
    fun provideReminderDao(database: MelhoreDatabase): ReminderDao = database.reminderDao()

    @Provides
    @Singleton
    fun provideCategoryDao(database: MelhoreDatabase): CategoryDao = database.categoryDao()

    @Provides
    @Singleton
    fun provideListDao(database: MelhoreDatabase): ListDao = database.listDao()

    @Provides
    @Singleton
    fun provideChecklistItemDao(database: MelhoreDatabase): ChecklistItemDao = database.checklistItemDao()
}
