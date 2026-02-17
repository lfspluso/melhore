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

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE reminders ADD COLUMN isRoutine INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE reminders ADD COLUMN customRecurrenceDays TEXT")
    }
}

private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // SQLite doesn't support adding foreign keys via ALTER TABLE, so we need to recreate the table
        // Step 1: Create new table with all columns including foreign keys
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS reminders_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                notes TEXT NOT NULL,
                type TEXT NOT NULL,
                dueAt INTEGER NOT NULL,
                categoryId INTEGER,
                listId INTEGER,
                priority TEXT NOT NULL,
                snoozedUntil INTEGER,
                status TEXT NOT NULL,
                isActive INTEGER NOT NULL,
                isRoutine INTEGER NOT NULL,
                customRecurrenceDays TEXT,
                parentReminderId INTEGER,
                startTime INTEGER,
                checkupFrequencyHours INTEGER,
                isTask INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL,
                FOREIGN KEY(listId) REFERENCES lists(id) ON DELETE SET NULL,
                FOREIGN KEY(parentReminderId) REFERENCES reminders(id) ON DELETE CASCADE
            )
        """.trimIndent())
        
        // Step 2: Copy data from old table to new table
        db.execSQL("""
            INSERT INTO reminders_new (
                id, title, notes, type, dueAt, categoryId, listId, priority, 
                snoozedUntil, status, isActive, isRoutine, customRecurrenceDays,
                parentReminderId, startTime, checkupFrequencyHours, isTask,
                createdAt, updatedAt
            )
            SELECT 
                id, title, notes, type, dueAt, categoryId, listId, priority,
                snoozedUntil, status, isActive, isRoutine, customRecurrenceDays,
                NULL, NULL, NULL, 0,
                createdAt, updatedAt
            FROM reminders
        """.trimIndent())
        
        // Step 3: Drop old table
        db.execSQL("DROP TABLE reminders")
        
        // Step 4: Rename new table to original name
        db.execSQL("ALTER TABLE reminders_new RENAME TO reminders")
        
        // Step 5: Recreate indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_categoryId ON reminders(categoryId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_listId ON reminders(listId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_dueAt ON reminders(dueAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_parentReminderId ON reminders(parentReminderId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_isTask ON reminders(isTask)")
    }
}

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Index on status for ACTIVE/COMPLETED/CANCELLED filtering
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_status ON reminders(status)")
        
        // Composite index for common query: status + dueAt
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_status_dueAt ON reminders(status, dueAt)")
        
        // Composite index for isTask + status filtering
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_isTask_status ON reminders(isTask, status)")
        
        // Index on startTime for task ordering
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_startTime ON reminders(startTime)")
        
        // Composite index for parentReminderId queries with ordering
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_parentReminderId_startTime_dueAt ON reminders(parentReminderId, startTime, dueAt)")
    }
}

private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add userId column (nullable for migration)
        db.execSQL("ALTER TABLE reminders ADD COLUMN userId TEXT")
        db.execSQL("ALTER TABLE categories ADD COLUMN userId TEXT")
        db.execSQL("ALTER TABLE checklist_items ADD COLUMN userId TEXT")
        // Backfill existing rows with default for pre-sign-in / local data
        db.execSQL("UPDATE reminders SET userId = 'local' WHERE userId IS NULL")
        db.execSQL("UPDATE categories SET userId = 'local' WHERE userId IS NULL")
        db.execSQL("UPDATE checklist_items SET userId = 'local' WHERE userId IS NULL")
        // Indexes for user-scoped queries (Sprint 17)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_userId ON reminders(userId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_userId_status_dueAt ON reminders(userId, status, dueAt)")
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
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7).build()

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
