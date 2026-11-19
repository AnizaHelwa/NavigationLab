package com.example.navigationlab.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.navigationlab.data.local.FavoriteStudentEntity
import com.example.navigationlab.data.repository.FavoriteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FavoritesViewModel(
    favoriteRepository: FavoriteRepository
) : ViewModel() {

    val favorites: StateFlow<List<FavoriteStudentEntity>> =
        favoriteRepository.observeFavorites()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    companion object {
        fun factory(
            favoriteRepository: FavoriteRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    return FavoritesViewModel(
                        favoriteRepository
                    ) as T
                }
            }
    }
}
