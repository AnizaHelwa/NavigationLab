package com.example.navigationlab.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.navigationlab.data.repository.StudentRepository
import com.example.navigationlab.data.model.Student

class HomeViewModel(
    private val repository: StudentRepository
) : ViewModel() {

    var uiState by mutableStateOf(HomeUiState())
        private set

    init {
        loadStudents()
    }

    fun onQueryChange(newQuery: String) {
        uiState = uiState.copy(searchQuery = newQuery)
    }

    fun search() {
        val query = uiState.searchQuery.trim()
        if (query.isEmpty()) {
            loadStudents()
        } else {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            repository.searchStudents(query) { result -> handleResult(result) }
        }
    }

    fun loadStudents() {
        uiState = uiState.copy(isLoading = true, errorMessage = null)
        repository.getStudents { result -> handleResult(result) }
    }

    private fun handleResult(result: Result<List<Student>>) {
        result
            .onSuccess { data ->
                uiState = uiState.copy(students = data, isLoading = false)
            }
            .onFailure { error ->
                uiState = uiState.copy(
                    students = emptyList(),
                    isLoading = false,
                    errorMessage = error.message ?: "Gagal mengambil data"
                )
            }
    }

    companion object {
        fun factory(repository: StudentRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(repository) as T
                }
            }
    }
}