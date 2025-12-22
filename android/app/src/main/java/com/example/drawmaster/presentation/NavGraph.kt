package com.example.drawmaster.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.drawmaster.presentation.screens.ConfirmImageScreen
import com.example.drawmaster.presentation.screens.GameOverScreen
import com.example.drawmaster.presentation.screens.LoginScreen
import com.example.drawmaster.presentation.screens.MainScreen
import com.example.drawmaster.presentation.screens.SelectImageScreen
import com.example.drawmaster.presentation.screens.ProfileScreen
import com.example.drawmaster.presentation.screens.ResultsScreen


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
        composable(route = "profile") {
            ProfileScreen(navController = navController)
        }
        composable(route = "friend_requests") {
            com.example.drawmaster.presentation.screens.FriendRequestsScreen(navController = navController)
        }
        composable(route = "select_friend") {
            com.example.drawmaster.presentation.screens.SelectFriendScreen(navController = navController)
        }
        composable(route = "multiplayer_waiting/{gameId}", arguments = listOf(navArgument("gameId") { type = NavType.StringType })) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            com.example.drawmaster.presentation.screens.MultiplayerWaitingScreen(navController = navController, gameId = gameId)
        }
        composable(route = "select_image") {
            SelectImageScreen(navController = navController)
        }
        // variant that accepts a gameId when invoked from multiplayer flow
        composable(route = "select_image/{gameId}", arguments = listOf(navArgument("gameId") { type = NavType.StringType; nullable = true })) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId")
            SelectImageScreen(navController = navController, gameId = gameId)
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
            ConfirmImageScreen(navController = navController, imageUriString = imageUri, gameId = null)
        }
        // confirm image when coming from multiplayer with gameId
        composable(route = "confirm_image/{imageUri}/{gameId}",
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType; nullable = true },
                navArgument("gameId") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val imageUri = backStackEntry.arguments?.getString("imageUri")
            val gameId = backStackEntry.arguments?.getString("gameId")
            ConfirmImageScreen(navController = navController, imageUriString = imageUri, gameId = gameId)
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
                imageUriString = imageUri,
                gameId = null
            )
        }
        // multiplayer variant with gameId
        composable(route = "game_screen/{imageUri}/{gameId}",
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType; nullable = true },
                navArgument("gameId") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val imageUri = backStackEntry.arguments?.getString("imageUri")
            val gameId = backStackEntry.arguments?.getString("gameId")
            com.example.drawmaster.presentation.screens.GameScreen(
                navController = navController,
                imageUriString = imageUri,
                gameId = gameId
            )
        }
        composable(route = "game_over_screen/{drawingUri}/{originalUri}",
            arguments = listOf(
                navArgument("drawingUri") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("originalUri") {
                    type = NavType.StringType
                    nullable = true
                }

            )
        ) { backStackEntry ->
            val drawingUri = backStackEntry.arguments?.getString("drawingUri")
            val originalUri = backStackEntry.arguments?.getString("originalUri")
            GameOverScreen(navController = navController, drawingUriString = drawingUri, originalUriString = originalUri)
        }
        composable(route = "results_screen/{score}",
            arguments = listOf(
                navArgument("score") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val calcscore = backStackEntry.arguments?.getInt("score") ?: 0
            ResultsScreen(navController = navController, score = calcscore)
        }
    }
}