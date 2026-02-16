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

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add status column, default to ACTIVE for existing records
        db.execSQL("ALTER TABLE reminders ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'")
        // Update status based on isActive: if isActive = 0, set status to CANCELLED, else ACTIVE
        db.execSQL("UPDATE reminders SET status = CASE WHEN isActive = 0 THEN 'CANCELLED' ELSE 'ACTIVE' END")
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
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()

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
