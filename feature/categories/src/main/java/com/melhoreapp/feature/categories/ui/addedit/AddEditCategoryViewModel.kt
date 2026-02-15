package com.melhoreapp.feature.categories.ui.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melhoreapp.core.database.dao.CategoryDao
import com.melhoreapp.core.database.entity.CategoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditCategoryViewModel @Inject constructor(
    private val categoryDao: CategoryDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val categoryId: Long? = savedStateHandle.get<Long>("categoryId")

    val isEdit: Boolean get() = categoryId != null

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        categoryId?.let { id ->
            viewModelScope.launch {
                categoryDao.getCategoryById(id)?.let { category ->
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
            if (categoryId != null) {
                categoryDao.getCategoryById(categoryId)?.let { existing ->
                    categoryDao.update(existing.copy(name = trimmed))
                }
            } else {
                categoryDao.insert(CategoryEntity(name = trimmed))
            }
            _saved.value = true
        }
    }
}
