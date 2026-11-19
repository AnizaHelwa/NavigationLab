
package com.example.navigationlab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

import com.example.navigationlab.data.local.DatabaseProvider
import com.example.navigationlab.data.remote.RetrofitClient
import com.example.navigationlab.data.remote.StudentRemoteDataSource
import com.example.navigationlab.data.repository.DefaultStudentRepository
import com.example.navigationlab.data.repository.FavoriteRepository
import com.example.navigationlab.data.preferences.UserPreferencesRepository

import com.example.navigationlab.ui.detail.DetailViewModel
import com.example.navigationlab.ui.home.HomeViewModel
import com.example.navigationlab.ui.screen.AboutScreen
import com.example.navigationlab.ui.screen.DetailScreen
import com.example.navigationlab.ui.screen.HomeScreen
import com.example.navigationlab.ui.screen.ProfileScreen
import com.example.navigationlab.ui.favorites.FavoritesViewModel
import androidx.compose.runtime.collectAsState
import com.example.navigationlab.ui.screen.FavoritesScreen

@Composable
fun NavigationLabApp() {

    val context = LocalContext.current

    val remoteDataSource = remember {
        StudentRemoteDataSource(RetrofitClient.apiService)
    }

    val repository = remember {
        DefaultStudentRepository(remoteDataSource)
    }

    // Repository untuk menyimpan preferensi pencarian
    val preferencesRepository = remember {
        UserPreferencesRepository(context)
    }

    // Repository untuk mengelola data mahasiswa favorit
    val favoriteRepository = remember {
        FavoriteRepository(
            DatabaseProvider.getDatabase(context)
                .favoriteStudentDao()
        )
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {

        composable(Routes.HOME) {

            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.factory(
                    repository,
                    preferencesRepository
                )
            )

            val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
            val searchQuery by homeViewModel.searchQuery.collectAsStateWithLifecycle()

            HomeScreen(
                uiState = homeUiState,
                searchQuery = searchQuery,
                onSearchQueryChange = homeViewModel::onSearchQueryChange,
                onRefresh = homeViewModel::refresh,
                onOpenDetail = { id ->
                    navController.navigate(Routes.detail(id))
                },
                onOpenProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onOpenAbout = {
                    navController.navigate(Routes.ABOUT)
                },
                onOpenFavorites = {
                    navController.navigate(Routes.FAVORITES)
                },
                onRetry = homeViewModel::retry
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(
                navArgument("studentId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->

            val studentId =
                backStackEntry.arguments?.getInt("studentId") ?: 0

            val viewModel: DetailViewModel = viewModel(
                factory = DetailViewModel.factory(
                    repository,
                    favoriteRepository
                )
            )

            val detailUiState by
            viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(studentId) {
                viewModel.loadStudent(studentId)
            }

            DetailScreen(
                uiState = detailUiState,
                onBack = {
                    navController.popBackStack()
                },
                onOpenProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onToggleFavorite = viewModel::toggleFavorite
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Routes.FAVORITES) {

            val favoritesViewModel: FavoritesViewModel = viewModel(
                factory = FavoritesViewModel.factory(favoriteRepository)
            )

            val favorites by favoritesViewModel.favorites
                .collectAsStateWithLifecycle()

            FavoritesScreen(
                favorites = favorites,
                onStudentClick = { studentId ->
                    navController.navigate(Routes.detail(studentId))
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
