package com.melhoreapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.melhoreapp.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY sortOrder ASC, name ASC")
    fun getAllCategories(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE userId = :userId AND id = :id")
    suspend fun getCategoryById(userId: String, id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Assigns all rows with userId = 'local' to the given user (e.g. after first sign-in). */
    @Query("UPDATE categories SET userId = :newUserId WHERE userId = 'local'")
    suspend fun migrateLocalUserIdTo(newUserId: String)

    /** Count of categories with userId = 'local' (Sprint 19 – migration detection). */
    @Query("SELECT COUNT(*) FROM categories WHERE userId = 'local'")
    suspend fun getLocalCategoryCount(): Int

    /** Deletes all categories with userId = 'local' (Sprint 19 – start fresh). */
    @Query("DELETE FROM categories WHERE userId = 'local'")
    suspend fun deleteAllLocalCategories()
}
