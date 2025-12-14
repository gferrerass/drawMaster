package com.example.drawmaster.presentation.screens


import androidx.compose.runtime.Composable
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val viewModel: MainViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("DrawMaster", color = Color.White)
                        val displayText = currentUser?.displayName ?: currentUser?.email
                        displayText?.let { name ->
                            Text(
                                text = "Welcome $name",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            authViewModel.signOut()
                            navController.navigate("login") {
                                popUpTo("main_screen") { inclusive = true }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
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
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val navController = rememberNavController()
    DrawMasterTheme {
        MainScreen(navController = navController)
    }
}
