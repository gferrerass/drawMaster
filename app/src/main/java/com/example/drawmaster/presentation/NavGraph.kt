package com.example.drawmaster.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable



@Composable
fun DrawMasterNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "main_screen",
        modifier = modifier
    ) {
        composable(route = "main_screen") {
            MainScreen(onSinglePlayer = { navController.navigate("select_image") })
        }
        composable(route = "select_image") {
            SelectImageScreen(navController = navController)
        }
        composable(route = "confirm_image") {
            ConfirmImageScreen(navController = navController)
        }
    }
}