package com.melhoreapp.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("listId"), Index("dueAt")]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val notes: String = "",
    val type: RecurrenceType = RecurrenceType.NONE,
    val dueAt: Long,
    val categoryId: Long? = null,
    val listId: Long? = null,
    val priority: Priority = Priority.MEDIUM,
    val snoozedUntil: Long? = null,
    val status: ReminderStatus = ReminderStatus.ACTIVE,
    val isActive: Boolean = true, // Kept for backward compatibility, synced with status
    val createdAt: Long,
    val updatedAt: Long
)
