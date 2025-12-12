package com.example.drawmaster.presentation.viewmodel

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
        navController.popBackStack(route = "main_screen", inclusive = false)
        /*
        val finalUri = _uiState.value.imageUri?.toString()

        if (finalUri != null) {
            val encodedUri = Uri.encode(finalUri)
            navController.navigate("drawing_screen/$encodedUri")
        } else {
            navController.popBackStack()
        }*/
    }

    fun onChooseDifferentImageClicked(navController: NavHostController) {
        navController.popBackStack()
    }
}