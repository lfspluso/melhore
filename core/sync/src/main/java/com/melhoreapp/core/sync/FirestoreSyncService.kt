package com.melhoreapp.core.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.melhoreapp.core.common.Result
import com.melhoreapp.core.database.entity.CategoryEntity
import com.melhoreapp.core.database.entity.ChecklistItemEntity
import com.melhoreapp.core.database.entity.Priority
import com.melhoreapp.core.database.entity.RecurrenceType
import com.melhoreapp.core.database.entity.ReminderEntity
import com.melhoreapp.core.database.entity.ReminderStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.ListenerRegistration
import javax.inject.Inject
import javax.inject.Singleton

private const val USERS = "users"
private const val REMINDERS = "reminders"
private const val CATEGORIES = "categories"
private const val CHECKLIST_ITEMS = "checklistItems"

/**
 * Service for syncing reminders, categories, and checklist items to/from Firebase Firestore.
 * Collections: /users/{userId}/reminders, /users/{userId}/categories, /users/{userId}/checklistItems.
 * Document ID = entity id (Room primary key) for consistent ids across devices.
 */
@Singleton
class FirestoreSyncService @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    fun uploadReminders(userId: String, reminders: List<ReminderEntity>): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        try {
            val col = firestore.collection(USERS).document(userId).collection(REMINDERS)
            reminders.forEach { r ->
                col.document(r.id.toString()).set(reminderToMap(r)).await()
            }
            emit(Result.Success(Unit))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Result.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    fun downloadReminders(userId: String): Flow<Result<List<ReminderEntity>>> = flow {
        emit(Result.Loading)
        try {
            val snapshot = firestore.collection(USERS).document(userId).collection(REMINDERS).get().await()
            val list = snapshot.documents.mapNotNull { doc -> doc.toReminderEntity(userId) }
            emit(Result.Success(list))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Result.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    fun uploadCategories(userId: String, categories: List<CategoryEntity>): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        try {
            val col = firestore.collection(USERS).document(userId).collection(CATEGORIES)
            categories.forEach { c ->
                col.document(c.id.toString()).set(categoryToMap(c)).await()
            }
            emit(Result.Success(Unit))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Result.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    fun downloadCategories(userId: String): Flow<Result<List<CategoryEntity>>> = flow {
        emit(Result.Loading)
        try {
            val snapshot = firestore.collection(USERS).document(userId).collection(CATEGORIES).get().await()
            val list = snapshot.documents.mapNotNull { doc -> doc.toCategoryEntity(userId) }
            emit(Result.Success(list))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Result.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    fun uploadChecklistItems(userId: String, items: List<ChecklistItemEntity>): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        try {
            val col = firestore.collection(USERS).document(userId).collection(CHECKLIST_ITEMS)
            items.forEach { item ->
                col.document(item.id.toString()).set(checklistItemToMap(item)).await()
            }
            emit(Result.Success(Unit))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Result.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    fun downloadChecklistItems(userId: String): Flow<Result<List<ChecklistItemEntity>>> = flow {
        emit(Result.Loading)
        try {
            val snapshot = firestore.collection(USERS).document(userId).collection(CHECKLIST_ITEMS).get().await()
            val list = snapshot.documents.mapNotNull { doc -> doc.toChecklistItemEntity(userId) }
            emit(Result.Success(list))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Result.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun deleteReminder(userId: String, id: Long): Result<Unit> = runCatching {
        firestore.collection(USERS).document(userId).collection(REMINDERS).document(id.toString()).delete().await()
        Result.Success(Unit)
    }.getOrElse { Result.Error(it) }

    suspend fun deleteCategory(userId: String, id: Long): Result<Unit> = runCatching {
        firestore.collection(USERS).document(userId).collection(CATEGORIES).document(id.toString()).delete().await()
        Result.Success(Unit)
    }.getOrElse { Result.Error(it) }

    suspend fun deleteChecklistItem(userId: String, id: Long): Result<Unit> = runCatching {
        firestore.collection(USERS).document(userId).collection(CHECKLIST_ITEMS).document(id.toString()).delete().await()
        Result.Success(Unit)
    }.getOrElse { Result.Error(it) }

    fun addRemindersListener(userId: String, onSnapshot: (List<ReminderEntity>) -> Unit): ListenerRegistration =
        firestore.collection(USERS).document(userId).collection(REMINDERS).addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { it.toReminderEntity(userId) } ?: emptyList()
            onSnapshot(list)
        }

    fun addCategoriesListener(userId: String, onSnapshot: (List<CategoryEntity>) -> Unit): ListenerRegistration =
        firestore.collection(USERS).document(userId).collection(CATEGORIES).addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { it.toCategoryEntity(userId) } ?: emptyList()
            onSnapshot(list)
        }

    fun addChecklistItemsListener(userId: String, onSnapshot: (List<ChecklistItemEntity>) -> Unit): ListenerRegistration =
        firestore.collection(USERS).document(userId).collection(CHECKLIST_ITEMS).addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { it.toChecklistItemEntity(userId) } ?: emptyList()
            onSnapshot(list)
        }

    private fun reminderToMap(r: ReminderEntity): Map<String, Any?> = mapOf(
        "id" to r.id,
        "userId" to r.userId,
        "title" to r.title,
        "notes" to r.notes,
        "type" to r.type.name,
        "dueAt" to r.dueAt,
        "categoryId" to r.categoryId,
        "listId" to r.listId,
        "priority" to r.priority.name,
        "snoozedUntil" to r.snoozedUntil,
        "status" to r.status.name,
        "isActive" to r.isActive,
        "isRoutine" to r.isRoutine,
        "customRecurrenceDays" to r.customRecurrenceDays,
        "parentReminderId" to r.parentReminderId,
        "startTime" to r.startTime,
        "checkupFrequencyHours" to r.checkupFrequencyHours,
        "isTask" to r.isTask,
        "createdAt" to r.createdAt,
        "updatedAt" to r.updatedAt
    )

    private fun categoryToMap(c: CategoryEntity): Map<String, Any?> = mapOf(
        "id" to c.id,
        "userId" to c.userId,
        "name" to c.name,
        "colorArgb" to c.colorArgb,
        "sortOrder" to c.sortOrder
    )

    private fun checklistItemToMap(item: ChecklistItemEntity): Map<String, Any?> = mapOf(
        "id" to item.id,
        "userId" to item.userId,
        "reminderId" to item.reminderId,
        "label" to item.label,
        "sortOrder" to item.sortOrder,
        "checked" to item.checked
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toReminderEntity(userId: String): ReminderEntity? {
    val id = getLong("id") ?: return null
    val title = getString("title") ?: return null
    val dueAt = getLong("dueAt") ?: return null
    val createdAt = getLong("createdAt") ?: return null
    val updatedAt = getLong("updatedAt") ?: return null
    return ReminderEntity(
        id = id,
        userId = getString("userId") ?: userId,
        title = title,
        notes = getString("notes") ?: "",
        type = getString("type")?.let { runCatching { RecurrenceType.valueOf(it) }.getOrNull() } ?: RecurrenceType.NONE,
        dueAt = dueAt,
        categoryId = getLong("categoryId"),
        listId = getLong("listId"),
        priority = getString("priority")?.let { runCatching { Priority.valueOf(it) }.getOrNull() } ?: Priority.MEDIUM,
        snoozedUntil = getLong("snoozedUntil"),
        status = getString("status")?.let { runCatching { ReminderStatus.valueOf(it) }.getOrNull() } ?: ReminderStatus.ACTIVE,
        isActive = getBoolean("isActive") ?: true,
        isRoutine = getBoolean("isRoutine") ?: false,
        customRecurrenceDays = getString("customRecurrenceDays"),
        parentReminderId = getLong("parentReminderId"),
        startTime = getLong("startTime"),
        checkupFrequencyHours = getLong("checkupFrequencyHours")?.toInt(),
        isTask = getBoolean("isTask") ?: false,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toCategoryEntity(userId: String): CategoryEntity? {
    val id = getLong("id") ?: return null
    val name = getString("name") ?: return null
    return CategoryEntity(
        id = id,
        userId = getString("userId") ?: userId,
        name = name,
        colorArgb = getLong("colorArgb")?.toInt(),
        sortOrder = getLong("sortOrder")?.toInt() ?: 0
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.toChecklistItemEntity(userId: String): ChecklistItemEntity? {
    val id = getLong("id") ?: return null
    val reminderId = getLong("reminderId") ?: return null
    val label = getString("label") ?: return null
    return ChecklistItemEntity(
        id = id,
        userId = getString("userId") ?: userId,
        reminderId = reminderId,
        label = label,
        sortOrder = getLong("sortOrder")?.toInt() ?: 0,
        checked = getBoolean("checked") ?: false
    )
}
