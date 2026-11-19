package com.example.navigationlab.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.navigationlab.data.repository.StudentRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import com.example.navigationlab.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first

class HomeViewModel(
    private val repository: StudentRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Sinyal "muat ulang sekarang" dari tombol Refresh dan Coba Lagi
    private val refreshTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        viewModelScope.launch {
            // 1. Mengambil query terakhir dari DataStore
            _searchQuery.value =
                preferencesRepository.lastSearchQuery.first()

            // 2. Menjalankan proses pencarian
            observeRequests()
        }

        // 3. Menyimpan perubahan query secara otomatis
        observeSearchQueryChanges()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun refresh() {
        viewModelScope.launch {
            refreshTrigger.emit(Unit)
        }
    }

    fun retry() {
        refresh()
    }

    @OptIn(FlowPreview::class)
    private fun observeRequests() {
        viewModelScope.launch {
            merge(
                searchQuery
                    .debounce(500)
                    .distinctUntilChanged(),
                refreshTrigger.map { searchQuery.value }
            ).collectLatest { query ->
                fetchStudents(query)
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQueryChanges() {
        viewModelScope.launch {
            searchQuery
                .debounce(500)
                .distinctUntilChanged()
                .collectLatest { value ->
                    preferencesRepository.saveLastSearchQuery(value)
                }
        }
    }

    private suspend fun fetchStudents(query: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        try {
            val data = if (query.isBlank()) {
                repository.getStudents()
            } else {
                repository.searchStudents(query)
            }
            _uiState.value = HomeUiState(students = data)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.value = HomeUiState(
                errorMessage = e.message ?: "Request gagal"
            )
        }
    }

    companion object {
        fun factory(
            repository: StudentRepository,
            preferencesRepository: UserPreferencesRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(
                        repository,
                        preferencesRepository
                    ) as T
                }
            }
    }
}