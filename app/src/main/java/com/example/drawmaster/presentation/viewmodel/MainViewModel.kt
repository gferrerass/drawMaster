package com.example.drawmaster.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController

class MainViewModel : ViewModel() {

    fun onSinglePlayerClicked(navController: NavHostController) {
        navController.navigate("select_image")
    }

    fun onMultiplayerClicked(navController: NavHostController) {
        // navController.navigate("select_friend")
    }
}