package com.melhoreapp.core.sync

import android.util.Log
import com.melhoreapp.core.common.Result
import com.melhoreapp.core.database.dao.CategoryDao
import com.melhoreapp.core.database.dao.ChecklistItemDao
import com.melhoreapp.core.database.dao.ReminderDao
import com.melhoreapp.core.database.entity.CategoryEntity
import com.melhoreapp.core.database.entity.ChecklistItemEntity
import com.melhoreapp.core.database.entity.ReminderEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.ListenerRegistration
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for syncing reminders, categories, and checklist items with Firebase Firestore.
 * Conflict resolution: cloud wins (for reminders compare [ReminderEntity.updatedAt]; categories/checklist overwrite).
 */
@Singleton
class SyncRepository @Inject constructor(
    private val firestoreSyncService: FirestoreSyncService,
    private val reminderDao: ReminderDao,
    private val categoryDao: CategoryDao,
    private val checklistItemDao: ChecklistItemDao
) {
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "MelhoreSync"
    }
    private val listenerRegistrations = AtomicReference<List<ListenerRegistration>>(emptyList())

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    /**
     * Full sync: download from Firestore, merge into Room (cloud wins), then upload local state to cloud.
     */
    fun syncAll(userId: String): Flow<Result<Unit>> = flow {
        _syncStatus.value = SyncStatus.Syncing
        emit(Result.Loading)
        Log.i(TAG, "syncAll starting for user $userId")
        try {
            withContext(Dispatchers.IO) {
                // Download and merge (cloud wins). Order must respect FKs: categories before reminders (categoryId), parents before children (parentReminderId).
                firestoreSyncService.downloadCategories(userId).first { it !is Result.Loading }.let { result ->
                    when (result) {
                        is Result.Success -> mergeCategories(userId, result.data)
                        is Result.Error -> throw result.exception
                        is Result.Loading -> { }
                    }
                }
                firestoreSyncService.downloadReminders(userId).first { it !is Result.Loading }.let { result ->
                    when (result) {
                        is Result.Success -> mergeReminders(userId, result.data)
                        is Result.Error -> throw result.exception
                        is Result.Loading -> { }
                    }
                }
                firestoreSyncService.downloadChecklistItems(userId).first { it !is Result.Loading }.let { result ->
                    when (result) {
                        is Result.Success -> mergeChecklistItems(userId, result.data)
                        is Result.Error -> throw result.exception
                        is Result.Loading -> { }
                    }
                }
                // Upload local state so cloud has latest (including any local-only or local-newer data)
                uploadAllInternal(userId)
            }
            Log.d(TAG, "syncAll complete for user $userId")
            _syncStatus.value = SyncStatus.Synced
            emit(Result.Success(Unit))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "syncAll failed for user $userId", e)
            _syncStatus.value = SyncStatus.Error(e.message)
            emit(Result.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Starts uploading all local reminders, categories, and checklist items to Firestore in the background.
     * Uses a long-lived scope so the upload is not cancelled when the caller (e.g. ViewModel) is cleared.
     * Call after any local create/update when you do not need to observe the result.
     */
    fun uploadAllInBackground(userId: String) {
        syncScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            try {
                uploadAllInternal(userId)
                Log.d(TAG, "Upload complete for user $userId")
                _syncStatus.value = SyncStatus.Synced
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Upload failed for user $userId", e)
                _syncStatus.value = SyncStatus.Error(e.message)
            }
        }
    }

    /**
     * Upload all local reminders, categories, and checklist items to Firestore.
     * Call after any local create/update.
     */
    fun uploadAll(userId: String): Flow<Result<Unit>> = flow {
        _syncStatus.value = SyncStatus.Syncing
        emit(Result.Loading)
        Log.i(TAG, "Upload starting for user $userId")
        try {
            withContext(Dispatchers.IO) {
                uploadAllInternal(userId)
            }
            Log.d(TAG, "Upload complete for user $userId")
            _syncStatus.value = SyncStatus.Synced
            emit(Result.Success(Unit))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Upload failed for user $userId", e)
            _syncStatus.value = SyncStatus.Error(e.message)
            emit(Result.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun uploadAllInternal(userId: String) {
        val reminders = reminderDao.getAllReminders(userId).first()
        val categories = categoryDao.getAllCategories(userId).first()
        val items = checklistItemDao.getAllItems(userId).first()
        firestoreSyncService.uploadReminders(userId, reminders).first { it !is Result.Loading }.let { r ->
            if (r is Result.Error) throw r.exception
        }
        firestoreSyncService.uploadCategories(userId, categories).first { it !is Result.Loading }.let { r ->
            if (r is Result.Error) throw r.exception
        }
        firestoreSyncService.uploadChecklistItems(userId, items).first { it !is Result.Loading }.let { r ->
            if (r is Result.Error) throw r.exception
        }
    }

    suspend fun deleteReminderFromCloud(userId: String, id: Long): Result<Unit> =
        firestoreSyncService.deleteReminder(userId, id)

    suspend fun deleteCategoryFromCloud(userId: String, id: Long): Result<Unit> =
        firestoreSyncService.deleteCategory(userId, id)

    suspend fun deleteChecklistItemFromCloud(userId: String, id: Long): Result<Unit> =
        firestoreSyncService.deleteChecklistItem(userId, id)

    /**
     * Start listening to Firestore for real-time updates; merge into Room (cloud wins).
     */
    fun enableAutoSync(userId: String) {
        disableAutoSync()
        val r1 = firestoreSyncService.addRemindersListener(userId) { list ->
            syncScope.launch { mergeReminders(userId, list) }
        }
        val r2 = firestoreSyncService.addCategoriesListener(userId) { list ->
            syncScope.launch { mergeCategories(userId, list) }
        }
        val r3 = firestoreSyncService.addChecklistItemsListener(userId) { list ->
            syncScope.launch { mergeChecklistItems(userId, list) }
        }
        listenerRegistrations.set(listOf(r1, r2, r3))
    }

    /**
     * Stop listening to Firestore.
     */
    fun disableAutoSync() {
        listenerRegistrations.getAndSet(emptyList()).forEach { it.remove() }
    }

    /**
     * Retry sync after error (Sprint 19). Call from UI when [syncStatus] is [SyncStatus.Error].
     */
    fun retrySync(userId: String): Flow<Result<Unit>> = syncAll(userId)

    private suspend fun mergeReminders(userId: String, cloud: List<ReminderEntity>) {
        // Insert parents before children (parentReminderId FK): non-tasks first, then tasks.
        val sorted = cloud.sortedBy { r -> if (r.parentReminderId == null) 0L else 1L }
        sorted.forEach { cloudReminder ->
            val local = reminderDao.getReminderById(cloudReminder.id)
            val takeCloud = local == null || cloudReminder.updatedAt >= local.updatedAt
            if (takeCloud) {
                // listId is not synced; clear to avoid FK to missing list (lists table is local-only).
                val withUserId = cloudReminder.copy(userId = userId, listId = null)
                if (local == null) reminderDao.insert(withUserId)
                else reminderDao.update(withUserId)
            }
        }
    }

    private suspend fun mergeCategories(userId: String, cloud: List<CategoryEntity>) {
        cloud.forEach { c ->
            val withUserId = c.copy(userId = userId)
            val existing = categoryDao.getCategoryById(userId, c.id)
            if (existing == null) categoryDao.insert(withUserId)
            else categoryDao.update(withUserId)
        }
    }

    private suspend fun mergeChecklistItems(userId: String, cloud: List<ChecklistItemEntity>) {
        cloud.forEach { item ->
            val withUserId = item.copy(userId = userId)
            checklistItemDao.insert(withUserId) // REPLACE on conflict
        }
    }
}
