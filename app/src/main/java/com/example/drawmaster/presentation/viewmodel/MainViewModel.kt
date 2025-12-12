package com.example.drawmaster.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MainUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun onSinglePlayerClicked(navController: NavHostController) {
        navController.navigate("select_image")
    }

    fun onMultiplayerClicked(navController: NavHostController) {
        // navController.navigate("select_friend")
    }
}