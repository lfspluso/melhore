package com.melhoreapp.feature.lists.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melhoreapp.core.database.dao.ListDao
import com.melhoreapp.core.database.entity.ListEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListListViewModel @Inject constructor(
    private val listDao: ListDao
) : ViewModel() {

    val lists: StateFlow<List<ListEntity>> = listDao.getAllLists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun deleteList(id: Long) {
        viewModelScope.launch {
            listDao.deleteById(id)
        }
    }
}
