package com.melhoreapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.melhoreapp.core.database.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE userId = :userId ORDER BY dueAt ASC")
    fun getAllReminders(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderEntity?

    /**
     * Gets active reminders with due time after the specified timestamp.
     * Optimized with composite index on (userId, status, dueAt).
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId AND status = 'ACTIVE' AND dueAt > :afterMillis ORDER BY dueAt ASC")
    suspend fun getUpcomingActiveReminders(userId: String, afterMillis: Long): List<ReminderEntity>

    /**
     * Gets all active reminders ordered by due date.
     * Optimized with index on status column.
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId AND status = 'ACTIVE' ORDER BY dueAt ASC")
    suspend fun getActiveReminders(userId: String): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE userId = :userId AND listId = :listId ORDER BY dueAt ASC")
    fun getRemindersByListId(userId: String, listId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE userId = :userId AND categoryId = :categoryId ORDER BY dueAt ASC")
    fun getRemindersByCategoryId(userId: String, categoryId: Long): Flow<List<ReminderEntity>>

    /** Call with non-empty list only; for empty selection use getAllReminders(). */
    @Query("SELECT * FROM reminders WHERE userId = :userId AND categoryId IN (:categoryIds) ORDER BY dueAt ASC")
    fun getRemindersByCategoryIds(userId: String, categoryIds: List<Long>): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Assigns all rows with userId = 'local' to the given user (e.g. after first sign-in). */
    @Query("UPDATE reminders SET userId = :newUserId WHERE userId = 'local'")
    suspend fun migrateLocalUserIdTo(newUserId: String)

    /** Count of reminders with userId = 'local' (Sprint 19 – migration detection). */
    @Query("SELECT COUNT(*) FROM reminders WHERE userId = 'local'")
    suspend fun getLocalReminderCount(): Int

    /** Deletes all reminders with userId = 'local' (Sprint 19 – start fresh). */
    @Query("DELETE FROM reminders WHERE userId = 'local'")
    suspend fun deleteAllLocalReminders()

    /**
     * Gets all task reminders for a parent Rotina reminder, ordered by start time and due date.
     * Optimized with composite index on (parentReminderId, startTime, dueAt).
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId AND parentReminderId = :parentReminderId ORDER BY startTime ASC, dueAt ASC")
    fun getTasksByParentReminderId(userId: String, parentReminderId: Long): Flow<List<ReminderEntity>>

    /**
     * Gets all task reminders for a parent Rotina reminder (one-time query).
     * Optimized with composite index on (parentReminderId, startTime, dueAt).
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId AND parentReminderId = :parentReminderId ORDER BY startTime ASC, dueAt ASC")
    suspend fun getTasksByParentReminderIdOnce(userId: String, parentReminderId: Long): List<ReminderEntity>

    /**
     * Gets all reminders excluding task reminders (isTask = 0).
     * Optimized with index on isTask column.
     */
    @Query("SELECT * FROM reminders WHERE userId = :userId AND isTask = 0 ORDER BY dueAt ASC")
    fun getAllRemindersExcludingTasks(userId: String): Flow<List<ReminderEntity>>
}
