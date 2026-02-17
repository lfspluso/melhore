package com.melhoreapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.melhoreapp.core.database.dao.CategoryDao
import com.melhoreapp.core.database.dao.ChecklistItemDao
import com.melhoreapp.core.database.dao.ListDao
import com.melhoreapp.core.database.dao.ReminderDao
import com.melhoreapp.core.database.entity.CategoryEntity
import com.melhoreapp.core.database.entity.ChecklistItemEntity
import com.melhoreapp.core.database.entity.ListEntity
import com.melhoreapp.core.database.entity.ReminderEntity

@Database(
    entities = [
        ReminderEntity::class,
        CategoryEntity::class,
        ListEntity::class,
        ChecklistItemEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class MelhoreDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun categoryDao(): CategoryDao
    abstract fun listDao(): ListDao
    abstract fun checklistItemDao(): ChecklistItemDao
}
