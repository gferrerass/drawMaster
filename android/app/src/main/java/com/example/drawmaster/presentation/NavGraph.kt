package com.example.drawmaster.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.drawmaster.presentation.screens.ConfirmImageScreen
import com.example.drawmaster.presentation.screens.LoginScreen
import com.example.drawmaster.presentation.screens.MainScreen
import com.example.drawmaster.presentation.screens.SelectImageScreen


@Composable
fun DrawMasterNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = modifier
    ) {
        composable(route = "login") {
            LoginScreen(navController = navController)
        }
        composable(route = "main_screen") {
            MainScreen(navController = navController)
        }
        composable(route = "select_image") {
            SelectImageScreen(navController = navController)
        }
        composable(route = "confirm_image/{imageUri}",
            arguments = listOf(
                navArgument("imageUri") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val imageUri = backStackEntry.arguments?.getString("imageUri")
            ConfirmImageScreen(navController = navController, imageUriString = imageUri)
        }
        composable(route = "game_screen/{imageUri}",
            arguments = listOf(
                navArgument("imageUri") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val imageUri = backStackEntry.arguments?.getString("imageUri")
            com.example.drawmaster.presentation.screens.GameScreen(
                navController = navController,
                imageUriString = imageUri
            )
        }
    }
}