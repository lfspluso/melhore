package com.melhoreapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.melhoreapp.core.database.entity.ListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {
    @Query("SELECT * FROM lists ORDER BY sortOrder ASC, name ASC")
    fun getAllLists(): Flow<List<ListEntity>>

    @Query("SELECT * FROM lists WHERE id = :id")
    suspend fun getListById(id: Long): ListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: ListEntity): Long

    @Update
    suspend fun update(list: ListEntity)

    @Query("DELETE FROM lists WHERE id = :id")
    suspend fun deleteById(id: Long)
}
