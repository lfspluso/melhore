package com.melhoreapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melhoreapp.core.auth.AuthRepository
import com.melhoreapp.core.common.Result
import com.melhoreapp.core.common.preferences.AppPreferences
import com.melhoreapp.core.database.dao.CategoryDao
import com.melhoreapp.core.database.dao.ChecklistItemDao
import com.melhoreapp.core.database.dao.ReminderDao
import com.melhoreapp.core.sync.MigrationHelper
import com.melhoreapp.core.sync.MigrationStrategy
import com.melhoreapp.core.sync.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AuthGateViewModel @Inject constructor(
    val authRepository: AuthRepository,
    private val appPreferences: AppPreferences,
    private val reminderDao: ReminderDao,
    private val categoryDao: CategoryDao,
    private val checklistItemDao: ChecklistItemDao,
    private val syncRepository: SyncRepository,
    private val migrationHelper: MigrationHelper
) : ViewModel() {

    val currentUser: StateFlow<com.melhoreapp.core.auth.CurrentUser?> = authRepository.currentUser

    private val _showMigrationDialog = MutableStateFlow(false)
    val showMigrationDialog: StateFlow<Boolean> = _showMigrationDialog.asStateFlow()

    private val _migrationInProgress = MutableStateFlow(false)
    val migrationInProgress: StateFlow<Boolean> = _migrationInProgress.asStateFlow()

    private val _migrationError = MutableStateFlow<String?>(null)
    val migrationError: StateFlow<String?> = _migrationError.asStateFlow()

    private var lastMigrationStrategy: MigrationStrategy? = null

    init {
        currentUser.onEach { user ->
            appPreferences.setLastUserId(user?.userId)
            if (user != null) {
                viewModelScope.launch {
                    if (migrationHelper.needsMigration(user.userId)) {
                        _showMigrationDialog.value = true
                    } else {
                        runPostMigrationSync(user.userId)
                    }
                }
            } else {
                _showMigrationDialog.value = false
                syncRepository.disableAutoSync()
            }
        }.launchIn(viewModelScope)
    }

    private suspend fun runPostMigrationSync(userId: String) = withContext(Dispatchers.IO) {
        migrateLocalDataToUser(userId)
        syncRepository.syncAll(userId).catch { }.collect { }
        syncRepository.enableAutoSync(userId)
    }

    private suspend fun migrateLocalDataToUser(userId: String) = withContext(Dispatchers.IO) {
        reminderDao.migrateLocalUserIdTo(userId)
        categoryDao.migrateLocalUserIdTo(userId)
        checklistItemDao.migrateLocalUserIdTo(userId)
    }

    fun onMigrationStrategyChosen(strategy: MigrationStrategy) {
        val userId = authRepository.currentUser.value?.userId ?: return
        lastMigrationStrategy = strategy
        _migrationError.value = null
        viewModelScope.launch {
            _migrationInProgress.value = true
            migrationHelper.executeMigration(userId, strategy).collect { result ->
                when (result) {
                    is Result.Success -> {
                        syncRepository.enableAutoSync(userId)
                        _showMigrationDialog.value = false
                        _migrationInProgress.value = false
                    }
                    is Result.Error -> {
                        _migrationError.value = result.exception.message ?: "Falha na sincronização"
                        _migrationInProgress.value = false
                    }
                    is Result.Loading -> { }
                }
            }
        }
    }

    fun retryMigration() {
        lastMigrationStrategy?.let { onMigrationStrategyChosen(it) }
    }

    fun clearMigrationError() {
        _migrationError.value = null
    }
}
