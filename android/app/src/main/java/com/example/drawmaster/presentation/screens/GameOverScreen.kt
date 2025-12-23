package com.example.drawmaster.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.drawmaster.R
import com.example.drawmaster.presentation.components.TextButton
import com.example.drawmaster.presentation.viewmodels.GameOverViewModel
import com.example.drawmaster.presentation.viewmodels.GameViewModel

import com.example.drawmaster.ui.theme.DrawMasterTheme
import com.example.drawmaster.ui.theme.TealBlue


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameOverScreen(
    navController: NavHostController,
    drawingUriString: String?,
    originalUriString: String?,
    gameId: String? = null,
    modifier: Modifier = Modifier
) {
    val gameOverVm: GameOverViewModel = viewModel()
    val gameVm: GameViewModel = viewModel()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // shared state for multiplayer results (populated when gameId is provided)
    var resultsMap by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var playersOrder by remember { mutableStateOf<List<String>>(emptyList()) }
    
    BackHandler(enabled = true) {
    } 
     
    if (!gameId.isNullOrBlank()) {
        // listen for server-written results in RTDB for the given gameId
        val database = try {
            val url = com.example.drawmaster.BuildConfig.FIREBASE_DB_URL
            if (url.isNullOrBlank()) com.google.firebase.database.FirebaseDatabase.getInstance() else com.google.firebase.database.FirebaseDatabase.getInstance(url)
        } catch (_: Exception) {
            com.google.firebase.database.FirebaseDatabase.getInstance()
        }
        DisposableEffect(gameId) {
            val ref = database.reference.child("games").child(gameId).child("results")
            val listener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val map = snapshot.value as? Map<String, Any?>
                    resultsMap = map
                    val scores = map?.get("scores") as? Map<*, *>
                    playersOrder = scores?.keys?.mapNotNull { it as? String } ?: emptyList()
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            }
            ref.addValueEventListener(listener)
            // If user hasn't submitted yet, try to submit current drawing (so server can compute once both submitted).
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                val subsRef = database.reference.child("games").child(gameId).child("submissions").child(uid)
                // read once
                subsRef.get().addOnSuccessListener { snap ->
                    if (!snap.exists()) {
                        // no submission for this user yet — if we have a drawingUri, send it to server
                        if (!drawingUriString.isNullOrBlank()) {
                            // compute score in background and submit with score (use runBlocking inside a thread)
                            Thread {
                                val sc = kotlinx.coroutines.runBlocking {
                                    com.example.drawmaster.presentation.scoring.ScoringUtil.computeScore(context, drawingUriString, originalUriString)
                                }
                                gameVm.submitMultiplayerDrawingForGame(gameId, drawingUriString, originalUriString ?: "", sc)
                            }.start()
                        }
                    } else {
                        // already submitted; ensure we listen for results
                        gameVm.listenForResults(gameId)
                    }
                }.addOnFailureListener {
                    // ignore read failure — still listen for results
                    gameVm.listenForResults(gameId)
                }
            }
            onDispose {
                try { ref.removeEventListener(listener) } catch (_: Exception) {}
            }
        }
    } else {
        // single-player: calculate score locally (or via the new endpoint inside the viewModel)
        val context = LocalContext.current
        DisposableEffect(drawingUriString, originalUriString) {
            gameOverVm.calculateScore(context, drawingUriString, originalUriString) 
            onDispose { }
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Game Over", color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = TealBlue)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            if (gameId.isNullOrBlank()) {
                // single-player view
                Spacer(modifier = Modifier.height(50.dp))
                Text(
                    text = "Your final drawing:",
                    color = Color.Gray,
                    style = MaterialTheme.typography.headlineSmall
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        val painter = if (drawingUriString != null) {
                            rememberAsyncImagePainter(model = drawingUriString)
                        } else {
                            painterResource(id = R.drawable.mountains)
                        }
                        Image(
                            painter = painter,
                            contentDescription = "Drawn picture",
                            modifier = Modifier.wrapContentSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    name = "See results",
                    backgroundColor = TealBlue,
                    borderColor = TealBlue,
                    fontColor = Color.White,
                    onClick = {
                        gameOverVm.navigatetoResults(navController)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                // multiplayer final view: show both drawings and scores
                val scores = resultsMap?.get("scores") as? Map<*, *> ?: emptyMap<Any, Any>()
                val drawingUris = resultsMap?.get("drawingUris") as? Map<*, *> ?: emptyMap<Any, Any>()
                val winnerUid = resultsMap?.get("winner") as? String
                val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

                // determine players list
                val playerUids: List<String> = if (playersOrder.isNotEmpty()) {
                    playersOrder
                } else {
                    // fallback: union of keys from drawingUris and scores, ensure uniqueness
                    val fallback = mutableListOf<String>()
                    for (k in drawingUris.keys) {
                        (k as? String)?.let { if (!fallback.contains(it)) fallback.add(it) }
                    }
                    for (k in scores.keys) {
                        (k as? String)?.let { if (!fallback.contains(it)) fallback.add(it) }
                    }
                    fallback
                }

                Text(text = "Final Results", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))

                // compute overall header (show once)
                val winnerUidLocal = winnerUid
                val resultHeader: String
                val headerColor: Color
                val myIsWinner = winnerUidLocal != null && winnerUidLocal == myUid
                if (winnerUidLocal == null) {
                    resultHeader = "It's a tie!"
                    headerColor = Color(0xFF757575)
                } else if (myIsWinner) {
                    resultHeader = "You won!"
                    headerColor = Color(0xFF2E7D32)
                } else {
                    resultHeader = "You lost — good luck next time"
                    headerColor = Color(0xFFC62828)
                }

                Text(
                    text = resultHeader,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = headerColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // show two cards vertically (mobile) with image + score
                val anyDrawingAvailable = drawingUris.values.mapNotNull { it as? String }.any { it.isNotBlank() }
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        for (uid in playerUids) {
                            val uri = drawingUris[uid] as? String
                            val score = (scores[uid] as? Number)?.toInt() ?: 0
                            val isMe = uid == myUid
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .padding(vertical = 8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    // avatar / thumbnail
                                    val avatarModifier = Modifier
                                        .size(40.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                    if (anyDrawingAvailable && !uri.isNullOrBlank()) {
                                        val painter = rememberAsyncImagePainter(model = uri)
                                        Image(painter = painter, contentDescription = null, modifier = avatarModifier)
                                    } else {
                                        val placeholder = painterResource(id = com.example.drawmaster.R.drawable.profile)
                                        Image(painter = placeholder, contentDescription = null, modifier = avatarModifier)
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                                        Text(text = if (isMe) "You" else "Opponent", style = MaterialTheme.typography.titleMedium)
                                    }

                                    // score on the right, prominent
                                    Text(
                                        text = score.toString(),
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            name = "Home",
                            backgroundColor = Color.White,
                            borderColor = TealBlue,
                            fontColor = TealBlue,
                            onClick = {
                                navController.navigate("main_screen") {
                                    popUpTo("main_screen") { inclusive = true }
                                }
                            }
                        )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameOverScreenPreview() {
    val navController = rememberNavController()
    DrawMasterTheme {
        GameOverScreen(navController = navController, drawingUriString = null, originalUriString = null)
    }
}