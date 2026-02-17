package com.melhoreapp.feature.categories.ui.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melhoreapp.core.auth.AuthRepository
import com.melhoreapp.core.database.dao.CategoryDao
import com.melhoreapp.core.database.entity.CategoryEntity
import com.melhoreapp.core.sync.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditCategoryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val categoryDao: CategoryDao,
    private val syncRepository: SyncRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val categoryId: Long? = savedStateHandle.get<Long>("categoryId")

    val isEdit: Boolean get() = categoryId != null

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private fun currentUserId(): String =
        authRepository.currentUser.value?.userId ?: "local"

    init {
        categoryId?.let { id ->
            viewModelScope.launch {
                val uid = currentUserId()
                categoryDao.getCategoryById(uid, id)?.let { category ->
                    _name.update { category.name }
                }
            }
        }
    }

    fun setName(value: String) {
        _name.value = value
    }

    fun save() {
        viewModelScope.launch {
            val trimmed = _name.value.trim()
            if (trimmed.isBlank()) return@launch
            val uid = currentUserId()
            if (categoryId != null) {
                categoryDao.getCategoryById(uid, categoryId)?.let { existing ->
                    categoryDao.update(existing.copy(name = trimmed))
                }
            } else {
                categoryDao.insert(CategoryEntity(userId = uid, name = trimmed))
            }
            if (uid != "local") syncRepository.uploadAllInBackground(uid)
            _saved.value = true
        }
    }
}
