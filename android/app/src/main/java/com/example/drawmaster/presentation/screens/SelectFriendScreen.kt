package com.example.drawmaster.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import com.example.drawmaster.ui.theme.TealBlue
import com.example.drawmaster.presentation.components.CustomButton
import androidx.compose.ui.res.painterResource
import com.example.drawmaster.R
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.drawmaster.presentation.viewmodels.FriendRequestsViewModel
import com.example.drawmaster.presentation.viewmodels.InviteViewModel
import com.example.drawmaster.domain.model.Friend
import androidx.compose.material3.Text
import androidx.compose.ui.draw.rotate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectFriendScreen(navController: NavHostController, modifier: Modifier = Modifier) {
    val friendsVm: FriendRequestsViewModel = viewModel()
    val inviteVm: InviteViewModel = viewModel()
    val friends by friendsVm.friends.collectAsState()
    val sending by inviteVm.sending.collectAsState()
    val error by inviteVm.error.collectAsState()

    LaunchedEffect(Unit) { friendsVm.load() }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Select Friend", color = MaterialTheme.colorScheme.onPrimary) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow),
                        contentDescription = "Go Back",
                        modifier = Modifier.size(24.dp).rotate(180f),
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = TealBlue)
        )
    }) { inner ->
        Box(modifier = Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.TopCenter) {
            if (friends.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text("You have no friends yet.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Use Friend Requests to add friends.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    items(friends) { f ->
                        CustomButton(
                            name = f.displayName ?: "Friend",
                            description = f.email ?: "",
                            image = painterResource(id = R.drawable.profile),
                            onClick = {
                                inviteVm.sendInvite(f.friendUid, f.displayName) { ok, gameId ->
                                    if (ok && gameId != null) {
                                        navController.navigate("multiplayer_waiting/$gameId")
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            if (sending) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            error?.let { err -> Text(text = err, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)) }
        }
    }
}

@Composable
fun FriendRow(friend: Friend, onInvite: (Friend) -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(6.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                Text(text = friend.displayName ?: "Friend")
                friend.email?.let { em ->
                    Text(text = em, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Button(onClick = { onInvite(friend) }) {
                Text("Invite")
            }
        }
    }
}
