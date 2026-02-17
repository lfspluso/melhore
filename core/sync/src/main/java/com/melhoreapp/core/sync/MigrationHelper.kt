package com.melhoreapp.core.sync

import android.util.Log
import com.melhoreapp.core.common.Result
import com.melhoreapp.core.common.preferences.AppPreferences
import com.melhoreapp.core.database.dao.CategoryDao
import com.melhoreapp.core.database.dao.ChecklistItemDao
import com.melhoreapp.core.database.dao.ReminderDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles first-time sign-in data migration: detect local data and run chosen strategy (Sprint 19).
 */
@Singleton
class MigrationHelper @Inject constructor(
    private val appPreferences: AppPreferences,
    private val reminderDao: ReminderDao,
    private val categoryDao: CategoryDao,
    private val checklistItemDao: ChecklistItemDao,
    private val syncRepository: SyncRepository
) {
    companion object {
        private const val TAG = "MelhoreMigration"
    }

    /**
     * True if we have local data (userId = 'local') and migration has not been completed for this user.
     */
    suspend fun needsMigration(userId: String): Boolean = withContext(Dispatchers.IO) {
        if (appPreferences.getMigrationCompletedForUser(userId)) return@withContext false
        val localReminders = reminderDao.getLocalReminderCount()
        val localCategories = categoryDao.getLocalCategoryCount()
        (localReminders > 0 || localCategories > 0)
    }

    /**
     * Runs the chosen migration strategy and marks migration complete on success.
     */
    fun executeMigration(userId: String, strategy: MigrationStrategy): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        Log.i(TAG, "executeMigration strategy=$strategy userId=$userId")
        try {
            withContext(Dispatchers.IO) {
                when (strategy) {
                    MigrationStrategy.UploadLocal -> {
                        reminderDao.migrateLocalUserIdTo(userId)
                        categoryDao.migrateLocalUserIdTo(userId)
                        checklistItemDao.migrateLocalUserIdTo(userId)
                        syncRepository.uploadAll(userId).first { it !is Result.Loading }.let { result ->
                            if (result is Result.Error) throw result.exception
                        }
                    }
                    MigrationStrategy.MergeWithCloud -> {
                        reminderDao.migrateLocalUserIdTo(userId)
                        categoryDao.migrateLocalUserIdTo(userId)
                        checklistItemDao.migrateLocalUserIdTo(userId)
                        syncRepository.syncAll(userId).first { it !is Result.Loading }.let { result ->
                            if (result is Result.Error) throw result.exception
                        }
                    }
                    MigrationStrategy.StartFresh -> {
                        checklistItemDao.deleteAllLocalChecklistItems()
                        reminderDao.deleteAllLocalReminders()
                        categoryDao.deleteAllLocalCategories()
                        syncRepository.syncAll(userId).first { it !is Result.Loading }.let { result ->
                            if (result is Result.Error) throw result.exception
                        }
                    }
                }
                appPreferences.setMigrationCompletedForUser(userId, true)
            }
            Log.d(TAG, "executeMigration complete for userId=$userId")
            emit(Result.Success(Unit))
        } catch (e: Exception) {
            Log.e(TAG, "executeMigration failed for userId=$userId", e)
            emit(Result.Error(e))
        }
    }.flowOn(Dispatchers.IO)
}
