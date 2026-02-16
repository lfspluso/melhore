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
    @Query("SELECT * FROM reminders ORDER BY dueAt ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderEntity?

    /**
     * Gets active reminders with due time after the specified timestamp.
     * Optimized with composite index on (status, dueAt).
     */
    @Query("SELECT * FROM reminders WHERE status = 'ACTIVE' AND dueAt > :afterMillis ORDER BY dueAt ASC")
    suspend fun getUpcomingActiveReminders(afterMillis: Long): List<ReminderEntity>

    /**
     * Gets all active reminders ordered by due date.
     * Optimized with index on status column.
     */
    @Query("SELECT * FROM reminders WHERE status = 'ACTIVE' ORDER BY dueAt ASC")
    suspend fun getActiveReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE listId = :listId ORDER BY dueAt ASC")
    fun getRemindersByListId(listId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE categoryId = :categoryId ORDER BY dueAt ASC")
    fun getRemindersByCategoryId(categoryId: Long): Flow<List<ReminderEntity>>

    /** Call with non-empty list only; for empty selection use getAllReminders(). */
    @Query("SELECT * FROM reminders WHERE categoryId IN (:categoryIds) ORDER BY dueAt ASC")
    fun getRemindersByCategoryIds(categoryIds: List<Long>): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Gets all task reminders for a parent Rotina reminder, ordered by start time and due date.
     * Optimized with composite index on (parentReminderId, startTime, dueAt).
     */
    @Query("SELECT * FROM reminders WHERE parentReminderId = :parentReminderId ORDER BY startTime ASC, dueAt ASC")
    fun getTasksByParentReminderId(parentReminderId: Long): Flow<List<ReminderEntity>>

    /**
     * Gets all task reminders for a parent Rotina reminder (one-time query).
     * Optimized with composite index on (parentReminderId, startTime, dueAt).
     */
    @Query("SELECT * FROM reminders WHERE parentReminderId = :parentReminderId ORDER BY startTime ASC, dueAt ASC")
    suspend fun getTasksByParentReminderIdOnce(parentReminderId: Long): List<ReminderEntity>

    /**
     * Gets all reminders excluding task reminders (isTask = 0).
     * Optimized with index on isTask column.
     */
    @Query("SELECT * FROM reminders WHERE isTask = 0 ORDER BY dueAt ASC")
    fun getAllRemindersExcludingTasks(): Flow<List<ReminderEntity>>
}
