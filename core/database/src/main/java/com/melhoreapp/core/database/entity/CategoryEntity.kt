package com.melhoreapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String? = "local", // Nullable in DB (migration 6→7); treat null as "local"
    val name: String,
    val colorArgb: Int? = null,
    val sortOrder: Int = 0
)
