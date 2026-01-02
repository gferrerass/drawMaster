package com.example.drawmaster.presentation.screens


import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.drawmaster.ui.theme.DrawMasterTheme
import com.example.drawmaster.R
import com.example.drawmaster.presentation.components.CustomButton
import com.example.drawmaster.ui.theme.TealBlue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.drawmaster.presentation.viewmodels.MainViewModel
import com.example.drawmaster.presentation.viewmodels.AuthViewModel
import com.example.drawmaster.presentation.viewmodels.InviteViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val viewModel: MainViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val inviteVm: InviteViewModel = viewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(currentUser?.uid) {
        try {
            val token = com.example.drawmaster.util.FirebaseTokenProvider.getToken(true)
            Log.i("TOKEN", token ?: "null")
        } catch (e: Exception) {
            Log.e("TOKEN", "Error obteniendo idToken", e)
        }
        if (currentUser?.uid != null) {
            inviteVm.startListeningForInvites()
        }
    }

    
    
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "DrawMaster",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                        val displayText = currentUser?.displayName ?: currentUser?.email
                        displayText?.let { name ->
                            Text(
                                text = "Welcome $name",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        // welcome text only (do not expose UIDs in UI)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = {
                            authViewModel.signOut()
                            navController.navigate("login") {
                                popUpTo("main_screen") { inclusive = true }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Log out",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = TealBlue
                )
            )
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Choose game mode",
                        color = Color.Gray,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CustomButton(
                        name = "Single Player",
                        description = "Practice your skills",
                        image = painterResource(id = R.drawable.profile),
                        onClick = { viewModel.onSinglePlayerClicked(navController) }
                    )
                    CustomButton(
                        name = "Multiplayer",
                        description = "Challenge your friends",
                        image = painterResource(id = R.drawable.multiplayer),
                        onClick = { viewModel.onMultiplayerClicked(navController) }
                    )
                    CustomButton(
                        name = "Friend Requests",
                        description = "View and accept requests",
                        image = painterResource(id = R.drawable.profile),
                        onClick = { navController.navigate("friend_requests") }
                    )
                }
            }
        }
    )

    // show incoming invite dialog
    val incoming by inviteVm.incoming.collectAsState()
    incoming?.let { inv ->
        AlertDialog(
            onDismissRequest = { /* keep until user acts */ },
            title = { Text(text = "Game Invite") },
            text = { Text(text = "${inv.fromName} has invited you to play.") },
            confirmButton = {
                TextButton(onClick = {
                    // capture invite gameId as fallback in case backend response omits it
                    val fallbackGameId = inv.gameId.takeIf { it.isNotBlank() }
                    inviteVm.acceptCurrentInvite { ok, gameId ->
                        val gid = gameId ?: fallbackGameId
                        if (ok && !gid.isNullOrBlank()) {
                            navController.navigate("multiplayer_waiting/$gid")
                        } else {
                            // fallback: show a toast or log to help debugging
                            android.util.Log.w("MainScreen", "accepted invite but no gameId returned or found")
                        }
                    }
                }) { Text("Accept") }
            },
            dismissButton = {
                TextButton(onClick = { inviteVm.rejectCurrentInvite() }) { Text("Reject") }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val navController = rememberNavController()
    DrawMasterTheme {
        MainScreen(navController = navController)
    }
}
