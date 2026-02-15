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

    @Query("SELECT * FROM reminders WHERE isActive = 1 AND dueAt > :afterMillis ORDER BY dueAt ASC")
    suspend fun getUpcomingActiveReminders(afterMillis: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isActive = 1 ORDER BY dueAt ASC")
    suspend fun getActiveReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE listId = :listId ORDER BY dueAt ASC")
    fun getRemindersByListId(listId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE categoryId = :categoryId ORDER BY dueAt ASC")
    fun getRemindersByCategoryId(categoryId: Long): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)
}
