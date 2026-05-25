package com.example.adrive.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adrive.data.model.TrashItem
import com.example.adrive.data.repository.DriveRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrashUiState(
    val items: List<TrashItem> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class TrashViewModel : ViewModel() {

    private val repo = DriveRepository()

    private val _state = MutableStateFlow(TrashUiState())
    val state: StateFlow<TrashUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repo.listTrash().fold(
                onSuccess = { items -> _state.update { it.copy(loading = false, items = items) } },
                onFailure = { _state.update { it.copy(loading = false, error = it.error) } }
            )
        }
    }

    fun restore(trashKey: String) {
        viewModelScope.launch {
            repo.restoreFromTrash(trashKey).onSuccess { refresh() }
        }
    }

    fun purge(trashKey: String) {
        viewModelScope.launch {
            repo.purgeTrashItem(trashKey).onSuccess { refresh() }
        }
    }

    fun purgeAll() {
        viewModelScope.launch {
            repo.purgeAllTrash().onSuccess { refresh() }
        }
    }
}

