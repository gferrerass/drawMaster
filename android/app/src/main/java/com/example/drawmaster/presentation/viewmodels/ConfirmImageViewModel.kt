package com.example.drawmaster.presentation.viewmodels

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ConfirmImageUiState(
    val imageUri: Uri? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ConfirmImageViewModel(
    imageUriString: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ConfirmImageUiState(
            imageUri = imageUriString?.toUri()
        )
    )
    val uiState: StateFlow<ConfirmImageUiState> = _uiState.asStateFlow()

    fun onStartDrawingClicked(navController: NavHostController) {
        val finalUri = _uiState.value.imageUri?.toString()

        if (finalUri != null) {
            val encodedUri = Uri.encode(finalUri)
            navController.navigate("game_screen/$encodedUri")
        } else {
            navController.popBackStack()
        }
    }

    fun onChooseDifferentImageClicked(navController: NavHostController) {
        navController.popBackStack()
    }
}