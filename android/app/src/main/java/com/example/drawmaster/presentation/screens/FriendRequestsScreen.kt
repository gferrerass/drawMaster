package com.example.drawmaster.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.drawmaster.R
import com.example.drawmaster.presentation.viewmodels.FriendRequestsViewModel
import com.example.drawmaster.ui.theme.TealBlue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.drawmaster.ui.theme.DrawMasterTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestsScreen(navController: NavController, viewModel: FriendRequestsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val requests by viewModel.requests.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val sendError by viewModel.sendError.collectAsState()
    val sendSuccess by viewModel.sendSuccess.collectAsState()
    val outgoing by viewModel.outgoing.collectAsState()
    val friends by viewModel.friends.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Search", "Pending")

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Friend Requests", color = Color.White)
                },
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = TealBlue
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .padding(innerPadding)
            .padding(16.dp)
            .fillMaxHeight(), verticalArrangement = Arrangement.Top) {

        TabRow(selectedTabIndex = selectedTab, modifier = Modifier.padding(top = 12.dp)) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }) {
                    Text(title, modifier = Modifier.padding(12.dp))
                }
            }
        }

        when (selectedTab) {
            0 -> SearchTab(onSend = { email -> viewModel.sendByEmail(email) }, sendError = sendError, sendSuccess = sendSuccess, outgoing = outgoing, onClear = { viewModel.clearSendMessages() })
            1 -> PendingTab(requests = requests, loading = loading, onAccept = { id -> viewModel.accept(id) }, onReject = { id -> viewModel.reject(id) })
            2 -> FriendsTab(friends = friends)
        }
    }
    }

    LaunchedEffect(error) {
        if (!error.isNullOrEmpty()) {
            snackbarHostState.showSnackbar(error ?: "Unknown error")
            viewModel.clearError()
        }
    }
}

@Composable
private fun SearchTab(onSend: (String) -> Unit, sendError: String?, sendSuccess: String?, outgoing: List<com.example.drawmaster.domain.model.FriendRequest>, onClear: () -> Unit) {
    var email by remember { mutableStateOf("") }
    Column(modifier = Modifier.padding(top = 12.dp)) {
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        if (!sendError.isNullOrEmpty()) {
            Text(sendError, color = androidx.compose.ui.graphics.Color.Red, modifier = Modifier.padding(top = 4.dp))
        }
        if (!sendSuccess.isNullOrEmpty()) {
            Text(sendSuccess, color = androidx.compose.ui.graphics.Color(0xFF2E7D32), modifier = Modifier.padding(top = 4.dp))
        }
        Button(onClick = { onSend(email) }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Send Friend Request")
        }

        // Outgoing pending requests (sent by user)
        if (outgoing.isNotEmpty()) {
            Text("Pending requests you sent:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(outgoing) { req ->
                    Text((req.displayName ?: "Pending") + " (sent)", modifier = Modifier.padding(vertical = 6.dp))
                }
            }
        }

        // Clear messages button
        if (!sendError.isNullOrEmpty() || !sendSuccess.isNullOrEmpty()) {
            Button(onClick = onClear, modifier = Modifier.padding(top = 8.dp)) { Text("Clear") }
        }
    }
}

@Composable
private fun FriendsTab(friends: List<com.example.drawmaster.domain.model.Friend>) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        if (friends.isEmpty()) {
            Text("No friends yet.", modifier = Modifier.padding(top = 8.dp))
        } else {
            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(friends) { f ->
                    Text(f.displayName ?: "Friend", modifier = Modifier.padding(vertical = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun PendingTab(requests: List<com.example.drawmaster.domain.model.FriendRequest>, loading: Boolean, onAccept: (Int) -> Unit, onReject: (Int) -> Unit) {
    if (loading) {
        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
    }

    LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
        items(requests) { req ->
            PendingRow(req.id, req.displayName ?: req.fromUid, onAccept, onReject)
        }
    }
}

@Composable
private fun PendingRow(id: Int, name: String, onAccept: (Int) -> Unit, onReject: (Int) -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, style = MaterialTheme.typography.bodyLarge)
        Row {
            Button(onClick = { onAccept(id) }, modifier = Modifier.padding(end = 8.dp)) { Text("Accept") }
            Button(onClick = { onReject(id) }) { Text("Reject") }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FriendRequestsScreenPreview() {
    val navController = rememberNavController()
    DrawMasterTheme {
        FriendRequestsScreen(navController = navController)
    }
}
