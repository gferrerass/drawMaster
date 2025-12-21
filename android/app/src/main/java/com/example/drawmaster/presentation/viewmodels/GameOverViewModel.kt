package com.example.drawmaster.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController

class GameOverViewModel : ViewModel() {

    val score: Int = 0

    fun calculateScore(drawingUriString: String?, originalUriString: String?) {

    }

    fun navigatetoResults(navController: NavHostController) {
        navController.navigate("results_screen/$score")
    }
}