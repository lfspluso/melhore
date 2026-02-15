package com.melhoreapp.feature.lists.ui.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melhoreapp.core.database.dao.ListDao
import com.melhoreapp.core.database.entity.ListEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditListViewModel @Inject constructor(
    private val listDao: ListDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val listId: Long? = savedStateHandle.get<Long>("listId")

    val isEdit: Boolean get() = listId != null

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        listId?.let { id ->
            viewModelScope.launch {
                listDao.getListById(id)?.let { list ->
                    _name.update { list.name }
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
            if (listId != null) {
                listDao.getListById(listId)?.let { existing ->
                    listDao.update(existing.copy(name = trimmed))
                }
            } else {
                listDao.insert(ListEntity(name = trimmed))
            }
            _saved.value = true
        }
    }
}
