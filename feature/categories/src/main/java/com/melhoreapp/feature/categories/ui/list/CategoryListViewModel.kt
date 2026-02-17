package com.melhoreapp.feature.categories.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melhoreapp.core.auth.AuthRepository
import com.melhoreapp.core.database.dao.CategoryDao
import com.melhoreapp.core.database.entity.CategoryEntity
import com.melhoreapp.core.sync.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoryListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val categoryDao: CategoryDao,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val userIdFlow = authRepository.currentUser
        .map { it?.userId ?: "local" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "local"
        )

    val categories: StateFlow<List<CategoryEntity>> = userIdFlow
        .flatMapLatest { uid -> categoryDao.getAllCategories(uid) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            val uid = userIdFlow.value
            categoryDao.deleteById(id)
            syncRepository.deleteCategoryFromCloud(uid, id)
        }
    }
}
