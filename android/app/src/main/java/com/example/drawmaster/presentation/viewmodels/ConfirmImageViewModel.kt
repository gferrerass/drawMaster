package com.example.drawmaster.presentation.viewmodels

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ConfirmImageUiState(
    val imageUri: Uri? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ConfirmImageViewModel(
    imageUriString: String?,
    private val gameId: String? = null
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
            // if this is a multiplayer game, update the RTDB game node so opponent starts too
            if (gameId != null) {
                try {
                    // use the same DB selection logic as elsewhere (respect BuildConfig.FIREBASE_DB_URL)
                    val database = try {
                        val url = com.example.drawmaster.BuildConfig.FIREBASE_DB_URL
                        if (url.isNullOrBlank()) FirebaseDatabase.getInstance() else FirebaseDatabase.getInstance(url)
                    } catch (_: Exception) {
                        FirebaseDatabase.getInstance()
                    }
                    val updates = mapOf<String, Any>(
                        "state" to "started",
                        "imageUri" to finalUri,
                        "startedAt" to System.currentTimeMillis()
                    )
                    database.reference.child("games").child(gameId).updateChildren(updates).addOnCompleteListener { task ->
                        // proceed to local game screen regardless of DB result, but log on failure
                        if (!task.isSuccessful) android.util.Log.w("ConfirmVM", "failed updating game node", task.exception)
                        // navigate and include gameId so the game screen can coordinate multiplayer
                        navController.navigate("game_screen/$encodedUri/$gameId")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("ConfirmVM", "error updating game node", e)
                    navController.navigate("game_screen/$encodedUri")
                }
            } else {
                navController.navigate("game_screen/$encodedUri")
            }
        } else {
            navController.popBackStack()
        }
    }

    fun onChooseDifferentImageClicked(navController: NavHostController) {
        navController.popBackStack()
    }
}