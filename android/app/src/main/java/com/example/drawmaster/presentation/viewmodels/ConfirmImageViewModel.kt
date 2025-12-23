package com.example.drawmaster.presentation.viewmodels

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import android.os.Handler
import android.os.Looper
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
    private val remoteUrlString: String? = null,
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
                // Prefer explicit remoteUrlString if provided (Unsplash remote URL)
                val remoteToUse = when {
                    !remoteUrlString.isNullOrBlank() -> remoteUrlString
                    finalUri.startsWith("http://") || finalUri.startsWith("https://") -> finalUri
                    else -> null
                }

                // If we have a remote URL, call backend API to set it centrally
                if (remoteToUse != null) {
                    try {
                        val user = FirebaseAuth.getInstance().currentUser
                        if (user == null) {
                            android.util.Log.w("ConfirmVM", "not authenticated; cannot call API to set reference")
                            navController.navigate("game_screen/$encodedUri/$gameId")
                            return
                        }
                        user.getIdToken(true).addOnCompleteListener { tokenTask ->
                            val idToken = tokenTask.result?.token ?: ""
                            val apiUrl = com.example.drawmaster.BuildConfig.API_BASE_URL.trimEnd('/') + "/multiplayer/game/$gameId/set_reference"
                            val client = OkHttpClient()
                            val json = "{\"imageUrl\": \"$remoteToUse\"}"
                            val mediaType = "application/json; charset=utf-8".toMediaType()
                            val body = json.toRequestBody(mediaType)
                            val req = Request.Builder()
                                .url(apiUrl)
                                .post(body)
                                .header("Authorization", "Bearer $idToken")
                                .header("Accept", "application/json")
                                .build()
                            val mainHandler = Handler(Looper.getMainLooper())
                            client.newCall(req).enqueue(object : okhttp3.Callback {
                                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                                    android.util.Log.w("ConfirmVM", "API set_reference call failed", e)
                                    mainHandler.post { navController.navigate("game_screen/$encodedUri/$gameId") }
                                }

                                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                                    response.use { r ->
                                        if (!r.isSuccessful) {
                                            android.util.Log.w("ConfirmVM", "API set_reference HTTP ${r.code}: ${r.message}")
                                        }
                                        mainHandler.post { navController.navigate("game_screen/$encodedUri/$gameId") }
                                    }
                                }
                            })
                        }
                        return
                    } catch (e: Exception) {
                        android.util.Log.w("ConfirmVM", "error calling backend set_reference", e)
                        // fallback to local DB write below
                    }
                }

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
                        if (!task.isSuccessful) android.util.Log.w("ConfirmVM", "failed updating game node", task.exception)
                        navController.navigate("game_screen/$encodedUri/$gameId")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("ConfirmVM", "error updating game node; navigating with gameId anyway", e)
                    try {
                        navController.navigate("game_screen/$encodedUri/$gameId")
                    } catch (ne: Exception) {
                        android.util.Log.w("ConfirmVM", "navigation with gameId failed; falling back to no-game route", ne)
                        navController.navigate("game_screen/$encodedUri")
                    }
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