package com.melhoreapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.melhoreapp.core.database.entity.ChecklistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistItemDao {
    @Query("SELECT * FROM checklist_items WHERE userId = :userId AND reminderId = :reminderId ORDER BY sortOrder ASC, id ASC")
    fun getItemsByReminderId(userId: String, reminderId: Long): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items WHERE userId = :userId AND reminderId = :reminderId ORDER BY sortOrder ASC, id ASC")
    suspend fun getItemsByReminderIdOnce(userId: String, reminderId: Long): List<ChecklistItemEntity>

    @Query("SELECT * FROM checklist_items WHERE userId = :userId")
    fun getAllItems(userId: String): Flow<List<ChecklistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ChecklistItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ChecklistItemEntity>)

    @Update
    suspend fun update(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM checklist_items WHERE userId = :userId AND reminderId = :reminderId")
    suspend fun deleteByReminderId(userId: String, reminderId: Long)

    /** Assigns all rows with userId = 'local' to the given user (e.g. after first sign-in). */
    @Query("UPDATE checklist_items SET userId = :newUserId WHERE userId = 'local'")
    suspend fun migrateLocalUserIdTo(newUserId: String)

    /** Deletes all checklist items with userId = 'local' (Sprint 19 – start fresh). Call before deleteAllLocalReminders. */
    @Query("DELETE FROM checklist_items WHERE userId = 'local'")
    suspend fun deleteAllLocalChecklistItems()
}
