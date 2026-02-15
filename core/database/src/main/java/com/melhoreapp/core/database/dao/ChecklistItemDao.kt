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
    @Query("SELECT * FROM checklist_items WHERE reminderId = :reminderId ORDER BY sortOrder ASC, id ASC")
    fun getItemsByReminderId(reminderId: Long): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items WHERE reminderId = :reminderId ORDER BY sortOrder ASC, id ASC")
    suspend fun getItemsByReminderIdOnce(reminderId: Long): List<ChecklistItemEntity>

    @Query("SELECT * FROM checklist_items")
    fun getAllItems(): Flow<List<ChecklistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ChecklistItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ChecklistItemEntity>)

    @Update
    suspend fun update(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM checklist_items WHERE reminderId = :reminderId")
    suspend fun deleteByReminderId(reminderId: Long)
}
