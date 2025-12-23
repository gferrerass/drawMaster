package com.example.drawmaster.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Composable
fun MultiplayerWaitingScreen(navController: NavHostController, gameId: String, modifier: Modifier = Modifier) {
    val auth = FirebaseAuth.getInstance()
    val database = try {
        val url = com.example.drawmaster.BuildConfig.FIREBASE_DB_URL
        if (url.isNullOrBlank()) FirebaseDatabase.getInstance() else FirebaseDatabase.getInstance(url)
    } catch (_: Exception) {
        FirebaseDatabase.getInstance()
    }

    var gameData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val ref = database.reference.child("games").child(gameId)
    DisposableEffect(gameId) {
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val map = snapshot.value as? Map<String, Any>
                gameData = map
            }

            override fun onCancelled(errorDB: com.google.firebase.database.DatabaseError) {
                error = "DB error: ${errorDB.message}"
            }
        }
        ref.addValueEventListener(listener)
        onDispose {
            try { ref.removeEventListener(listener) } catch (_: Exception) {}
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Waiting for opponent to accept...", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        if (error != null) Text(text = error!!, color = MaterialTheme.colorScheme.error)

        val myUid = FirebaseAuth.getInstance().currentUser?.uid
        val playerA = gameData?.get("playerA") as? String
        val playerB = gameData?.get("playerB") as? String

        if (playerB == null) {
            // still waiting
            CircularProgressIndicator()
        } else {
            // opponent accepted
            Text(text = "Opponent joined")
            Spacer(modifier = Modifier.height(12.dp))
            if (myUid == playerA) {
                // player A should pick image now
                Button(onClick = { navController.navigate("select_image/$gameId") }) {
                    Text("Select image and start")
                }
            } else {
                Text(text = "Waiting for player A to select image...")
                // if game state indicates start and image available, navigate to game screen
                val state = gameData?.get("state") as? String
                val imageUri = gameData?.get("imageUri") as? String
                if (state == "started" && !imageUri.isNullOrBlank()) {
                    val encodedUri = java.net.URLEncoder.encode(imageUri, "UTF-8")
                    LaunchedEffect(gameId) {
                        // Pass gameId so the GameScreen knows this is a multiplayer match
                        val encodedGameId = java.net.URLEncoder.encode(gameId, "UTF-8")
                        navController.navigate("game_screen/$encodedUri/$encodedGameId")
                    }
                }
            }
        }
    }
}
