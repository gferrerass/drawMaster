package com.example.drawmaster.presentation.viewmodel

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController

class ConfirmImageViewModel(
    imageUriString: String?
) : ViewModel() {

    val imageUri: Uri? = imageUriString?.toUri()

    fun onStartDrawingClicked(navController: NavHostController) {
        navController.popBackStack(route = "main_screen", inclusive = false)
        /*
        val finalUri = imageUri?.toString()

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