package com.example.drawmaster.presentation.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.example.drawmaster.presentation.components.DrawingStroke
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import android.os.Handler
import android.os.Looper

// 3 possible states: Loading, Playing and Finished
sealed class GameScreenState {
    object Loading : GameScreenState()
    data class Playing(
        val timeRemaining: Int,
        val hasDrawn: Boolean = false
    ) : GameScreenState()
    object Finished : GameScreenState()
    object WaitingForResults : GameScreenState()
    data class Error(val message: String) : GameScreenState()
}
class GameViewModel : ViewModel() {

    private val _gameState = MutableStateFlow<GameScreenState>(GameScreenState.Loading)
    val gameState: StateFlow<GameScreenState> = _gameState.asStateFlow()

    private val _strokes = MutableStateFlow<List<DrawingStroke>>(emptyList())
    val strokes: StateFlow<List<DrawingStroke>> = _strokes.asStateFlow()

    private val _imageName = MutableStateFlow<String?>(null)
    val imageName: StateFlow<String?> = _imageName.asStateFlow()

    private val _strokeColor = MutableStateFlow(android.graphics.Color.BLACK)
    val strokeColor: StateFlow<Int> = _strokeColor.asStateFlow()

    private val _strokeWidth = MutableStateFlow(13f)
    val strokeWidth: StateFlow<Float> = _strokeWidth.asStateFlow()

    private val _canvasWidth = MutableStateFlow(0)
    val canvasWidth: StateFlow<Int> = _canvasWidth.asStateFlow()

    private val _canvasHeight = MutableStateFlow(0)
    val canvasHeight: StateFlow<Int> = _canvasHeight.asStateFlow()

    private var timerJob: Job? = null
    private val totalGameTime = 60 // seconds
    private var multiplayerGameId: String? = null
    private var submissionsListener: com.google.firebase.database.ValueEventListener? = null
    private var resultsListener: com.google.firebase.database.ValueEventListener? = null
    private val _results = MutableStateFlow<Map<String, Any?>?>(null)
    val results: StateFlow<Map<String, Any?>?> = _results.asStateFlow()
    private val httpClient = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun startGame(imageName: String = "drawing_challenge", gameId: String? = null) {
        _imageName.value = imageName
        _strokes.value = emptyList()
        multiplayerGameId = gameId
        if (multiplayerGameId != null) {
            // start listening for results if they appear
            listenForResults(multiplayerGameId!!)
        }
        startTimer()
    }

    fun onDrawingChanged(newStrokes: List<DrawingStroke>) {
        _strokes.value = newStrokes
        
        // Updating state on the first stroke
        val currentState = _gameState.value
        if (currentState is GameScreenState.Playing && !currentState.hasDrawn && newStrokes.isNotEmpty()) {
            _gameState.value = GameScreenState.Playing(
                timeRemaining = currentState.timeRemaining,
                hasDrawn = true
            )
        }
    }

    fun clearDrawing() {
        _strokes.value = emptyList()
        val currentState = _gameState.value
        if (currentState is GameScreenState.Playing) {
            _gameState.value = GameScreenState.Playing(
                timeRemaining = currentState.timeRemaining,
                hasDrawn = false
            )
        }
    }

    fun undoLastStroke() {
        val strokes = _strokes.value.toMutableList()
        if (strokes.isNotEmpty()) {
            strokes.removeAt(strokes.size - 1)
            _strokes.value = strokes
            onDrawingChanged(strokes)
        }
    }

    fun setStrokeColor(color: Int) {
        _strokeColor.value = color
    }

    fun setStrokeWidth(width: Float) {
        _strokeWidth.value = width
    }

    fun setCanvasSize(width: Int, height: Int) {
        _canvasWidth.value = width
        _canvasHeight.value = height
    }

    fun finishGame(score: Float = 0f) {
        timerJob?.cancel()
        if (multiplayerGameId == null) {
            _gameState.value = GameScreenState.Finished
        } else {
            // in multiplayer, if user didn't press Submit and time expired, write a placeholder submission
            val gameId = multiplayerGameId!!
            try {
                val database = try {
                    val url = com.example.drawmaster.BuildConfig.FIREBASE_DB_URL
                    if (url.isNullOrBlank()) com.google.firebase.database.FirebaseDatabase.getInstance() else com.google.firebase.database.FirebaseDatabase.getInstance(url)
                } catch (_: Exception) {
                    com.google.firebase.database.FirebaseDatabase.getInstance()
                }
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: run {
                    _gameState.value = GameScreenState.Error("not authenticated")
                    return
                }
                val submissionsRef = database.reference.child("games").child(gameId).child("submissions")
                // placeholder payload — drawingUri empty signifies timeout/no drawing yet
                val payload = mapOf(
                    "drawingUri" to null,
                    "originalUri" to _imageName.value,
                    "submittedAt" to System.currentTimeMillis(),
                    "timedOut" to true
                )
                submissionsRef.child(uid).setValue(payload)
                _gameState.value = GameScreenState.WaitingForResults
                // ensure computation runs (will compute when two submissions are present)
                observeSubmissionsAndCompute(gameId)
            } catch (e: Exception) {
                _gameState.value = GameScreenState.Error("failed submitting timeout: ${e.message}")
            }
        }
    }

    fun navigatetoGameOverScreen(navController: NavHostController, drawingURI: String, originalURI: String, gameId: String? = null) {
        val encodedUri = Uri.encode(drawingURI)
        val encodedOriginalUri = Uri.encode(originalURI)
        if (gameId.isNullOrBlank()) {
            navController.navigate("game_over_screen/$encodedUri/$encodedOriginalUri")
        } else {
            val encodedGameId = Uri.encode(gameId)
            navController.navigate("game_over_screen/$encodedUri/$encodedOriginalUri/$encodedGameId")
        }
    }

    fun submitMultiplayerDrawing(drawingURI: String, originalURI: String) {
        val gameId = multiplayerGameId ?: return
        try {
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (user == null) {
                _gameState.value = GameScreenState.Error("not authenticated")
                return
            }
            user.getIdToken(true).addOnCompleteListener { tokenTask ->
                if (!tokenTask.isSuccessful) {
                    mainHandler.post { _gameState.value = GameScreenState.Error("token error: ${tokenTask.exception?.message}") }
                    return@addOnCompleteListener
                }
                val idToken = tokenTask.result?.token ?: ""
                val apiUrl = com.example.drawmaster.BuildConfig.API_BASE_URL.trimEnd('/') + "/multiplayer/game/$gameId/submit"
                val json = org.json.JSONObject().apply {
                    put("drawingUri", drawingURI)
                    put("originalUri", originalURI)
                    put("timedOut", false)
                }.toString()
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = json.toRequestBody(mediaType)
                val req = Request.Builder()
                    .url(apiUrl)
                    .post(body)
                    .header("Authorization", "Bearer $idToken")
                    .header("Accept", "application/json")
                    .build()
                httpClient.newCall(req).enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                        mainHandler.post { _gameState.value = GameScreenState.Error("submit failed: ${e.message}") }
                    }

                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        response.use { r ->
                            val bodyStr = r.body?.string()
                            if (!r.isSuccessful) {
                                mainHandler.post { _gameState.value = GameScreenState.Error("submit HTTP ${r.code}: ${r.message}") }
                                return
                            }
                            // submission accepted by server; wait for results
                            mainHandler.post {
                                _gameState.value = GameScreenState.WaitingForResults
                                // ensure we listen for results written by server
                                listenForResults(gameId)
                            }
                        }
                    }
                })
            }
        } catch (e: Exception) {
            _gameState.value = GameScreenState.Error("failed submitting drawing: ${e.message}")
        }
    }

    private fun observeSubmissionsAndCompute(gameId: String) {
        try {
            val database = try {
                val url = com.example.drawmaster.BuildConfig.FIREBASE_DB_URL
                if (url.isNullOrBlank()) com.google.firebase.database.FirebaseDatabase.getInstance() else com.google.firebase.database.FirebaseDatabase.getInstance(url)
            } catch (_: Exception) {
                com.google.firebase.database.FirebaseDatabase.getInstance()
            }
            val submissionsRef = database.reference.child("games").child(gameId).child("submissions")
            // listen once for current submissions and on change
            submissionsListener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    // require at least two child submissions
                    if (snapshot.childrenCount >= 2) {
                        val submissions = mutableListOf<Pair<String, Map<String, Any?>>>()
                        for (child in snapshot.children) {
                            val uid = child.key ?: continue
                            val valMap = child.value as? Map<*, *> ?: continue
                            // convert values to String? map
                            val castMap = mutableMapOf<String, Any?>()
                            for ((k, v) in valMap) {
                                if (k is String) castMap[k] = v
                            }
                            submissions.add(uid to castMap)
                        }
                        if (submissions.size >= 2) {
                            // take first two submissions
                            val (uidA, subA) = submissions[0]
                            val (uidB, subB) = submissions[1]
                            val drawingA = subA["drawingUri"] as? String
                            val drawingB = subB["drawingUri"] as? String
                            // placeholder scoring: both zero for now
                            val scoreA = 0
                            val scoreB = 0
                            val results = mapOf<String, Any?>(
                                "scores" to mapOf(uidA to scoreA, uidB to scoreB),
                                "drawingUris" to mapOf(uidA to drawingA, uidB to drawingB),
                                "winner" to when {
                                    scoreA > scoreB -> uidA
                                    scoreB > scoreA -> uidB
                                    else -> null
                                }
                            )
                            // write results and set state
                            database.reference.child("games").child(gameId).child("results").setValue(results)
                            database.reference.child("games").child(gameId).child("state").setValue("results")
                        }
                    }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            }
            submissionsRef.addValueEventListener(submissionsListener!!)
        } catch (e: Exception) {
            android.util.Log.w("GameVM", "observeSubmissions error", e)
        }
    }

    private fun listenForResults(gameId: String) {
        try {
            val database = try {
                val url = com.example.drawmaster.BuildConfig.FIREBASE_DB_URL
                if (url.isNullOrBlank()) com.google.firebase.database.FirebaseDatabase.getInstance() else com.google.firebase.database.FirebaseDatabase.getInstance(url)
            } catch (_: Exception) {
                com.google.firebase.database.FirebaseDatabase.getInstance()
            }
            val resultsRef = database.reference.child("games").child(gameId).child("results")
            resultsListener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    if (!snapshot.exists()) return
                    val out = mutableMapOf<String, Any?>()
                    // If the results node contains primitive map keys, snapshot.value may be a Map
                    val snapVal = snapshot.value
                    if (snapVal is Map<*, *>) {
                        for ((k, v) in snapVal) {
                            if (k is String) out[k] = v
                        }
                    } else {
                        // fallback: build from children
                        for (child in snapshot.children) {
                            val key = child.key ?: continue
                            out[key] = child.value
                        }
                    }
                    _results.value = out
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            }
            resultsRef.addValueEventListener(resultsListener!!)
        } catch (e: Exception) {
            android.util.Log.w("GameVM", "listenForResults error", e)
        }
    }
    private fun startTimer() {
        timerJob?.cancel()
        _gameState.value = GameScreenState.Playing(timeRemaining = totalGameTime)

        timerJob = viewModelScope.launch {
            for (i in totalGameTime downTo 1) {
                _gameState.value = GameScreenState.Playing(
                    timeRemaining = i,
                    hasDrawn = (_strokes.value.isNotEmpty())
                )
                delay(1000)
            }
            // Time's up
            finishGame(score = 0f)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        // remove any firebase listeners
        if (submissionsListener != null && multiplayerGameId != null) {
            val database = try {
                val url = com.example.drawmaster.BuildConfig.FIREBASE_DB_URL
                if (url.isNullOrBlank()) com.google.firebase.database.FirebaseDatabase.getInstance() else com.google.firebase.database.FirebaseDatabase.getInstance(url)
            } catch (_: Exception) {
                com.google.firebase.database.FirebaseDatabase.getInstance()
            }
            try { database.reference.child("games").child(multiplayerGameId!!).child("submissions").removeEventListener(submissionsListener!!) } catch (_: Exception) {}
        }
        if (resultsListener != null && multiplayerGameId != null) {
            val database = try {
                val url = com.example.drawmaster.BuildConfig.FIREBASE_DB_URL
                if (url.isNullOrBlank()) com.google.firebase.database.FirebaseDatabase.getInstance() else com.google.firebase.database.FirebaseDatabase.getInstance(url)
            } catch (_: Exception) {
                com.google.firebase.database.FirebaseDatabase.getInstance()
            }
            try { database.reference.child("games").child(multiplayerGameId!!).child("results").removeEventListener(resultsListener!!) } catch (_: Exception) {}
        }
    }
}
