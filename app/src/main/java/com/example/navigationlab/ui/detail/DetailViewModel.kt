
package com.example.navigationlab.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.navigationlab.data.repository.StudentRepository
import com.example.navigationlab.data.repository.FavoriteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailViewModel(
    private val repository: StudentRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var favoriteJob: Job? = null

    fun loadStudent(id: Int) {
        viewModelScope.launch {
            _uiState.value = DetailUiState(isLoading = true)

            try {
                val student = repository.getStudent(id)
                _uiState.value = DetailUiState(student = student)

                observeFavorite(id)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = DetailUiState(
                    errorMessage = e.message ?: "Data tidak tersedia"
                )
            }
        }
    }

    private fun observeFavorite(id: Int) {
        favoriteJob?.cancel()

        favoriteJob = viewModelScope.launch {
            favoriteRepository.observeIsFavorite(id).collect { value ->
                _uiState.update {
                    it.copy(isFavorite = value)
                }
            }
        }
    }

    fun toggleFavorite() {
        val student = uiState.value.student ?: return

        viewModelScope.launch {
            if (uiState.value.isFavorite) {
                favoriteRepository.remove(student.id)
            } else {
                favoriteRepository.add(student)
            }
        }
    }

    companion object {
        fun factory(
            repository: StudentRepository,
            favoriteRepository: FavoriteRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    return DetailViewModel(
                        repository,
                        favoriteRepository
                    ) as T
                }
            }
    }
}
