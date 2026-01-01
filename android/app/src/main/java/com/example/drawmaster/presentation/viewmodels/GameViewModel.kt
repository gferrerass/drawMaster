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
    private val totalGameTime = 15 // seconds
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
            return
        }

        // Multiplayer: if the player has drawn something and canvas size is known,
        // prefer to set state to Finished so the UI can generate the bitmap and
        // call submitMultiplayerDrawing which will include the computed score.
        val hasStrokes = _strokes.value.isNotEmpty()
        val cw = _canvasWidth.value
        val ch = _canvasHeight.value

        if (hasStrokes && cw > 0 && ch > 0) {
            _gameState.value = GameScreenState.Finished
            return
        }

        // No drawing to submit: call the server submit endpoint with score=0 so the server
        // (with admin privileges) writes submissions and computes results reliably.
        val gameId = multiplayerGameId!!
        try {
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (user == null) {
                _gameState.value = GameScreenState.Error("not authenticated")
                return
            }
            // Use the existing helper to submit via backend; this handles token and result listening.
            viewModelScope.launch {
                try {
                    submitMultiplayerDrawingForGame(gameId, "", _imageName.value ?: "", 0)
                    // submitMultiplayerDrawingForGame will set WaitingForResults on success
                } catch (e: Exception) {
                    mainHandler.post { _gameState.value = GameScreenState.Error("failed submitting timeout: ${e.message}") }
                }
            }
        } catch (e: Exception) {
            _gameState.value = GameScreenState.Error("failed submitting timeout: ${e.message}")
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

    fun submitMultiplayerDrawing(drawingURI: String, originalURI: String, score: Int? = null) {
        val gameId = multiplayerGameId ?: return
        viewModelScope.launch {
            try {
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    _gameState.value = GameScreenState.Error("not authenticated")
                    return@launch
                }
                val idToken = try { user.getIdToken(true).result?.token ?: "" } catch (_: Exception) { "" }
                val apiUrl = com.example.drawmaster.BuildConfig.API_BASE_URL.trimEnd('/') + "/multiplayer/game/$gameId/submit"
                val json = org.json.JSONObject().apply {
                    put("drawingUri", drawingURI)
                    put("originalUri", originalURI)
                    put("timedOut", false)
                    if (score != null) put("score", score)
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
                            if (!r.isSuccessful) {
                                mainHandler.post { _gameState.value = GameScreenState.Error("submit HTTP ${r.code}: ${r.message}") }
                                return
                            }
                            mainHandler.post {
                                _gameState.value = GameScreenState.WaitingForResults
                                listenForResults(gameId)
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                _gameState.value = GameScreenState.Error("failed submitting drawing: ${e.message}")
            }
        }
    }

    /**
     * Submit a drawing for the specified multiplayer game id. Used by screens that only have the
     * gameId and didn't set `multiplayerGameId` on this ViewModel.
     */
    fun submitMultiplayerDrawingForGame(gameId: String, drawingURI: String, originalURI: String) {
        // convenience wrapper that allows callers to optionally provide a computed score
        submitMultiplayerDrawingForGame(gameId, drawingURI, originalURI, null)
    }

    fun submitMultiplayerDrawingForGame(gameId: String, drawingURI: String, originalURI: String, score: Int?) {
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
                    if (score != null) put("score", score)
                }.toString()
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = json.toRequestBody(mediaType)
                val req = Request.Builder()
                    .url(apiUrl)
                    .post(body)
                    .header("Authorization", "Bearer $idToken")
                    .header("Accept", "application/json")
                    .build()
                // helper to build request with token
                fun buildRequest(token: String): Request {
                    return Request.Builder()
                        .url(apiUrl)
                        .post(body)
                        .header("Authorization", "Bearer $token")
                        .header("Accept", "application/json")
                        .build()
                }

                android.util.Log.i("GameVM", "submitting multiplayer drawing to $apiUrl for game=$gameId uid=${user.uid} score=${score ?: "<null>"} drawingUri=$drawingURI")

                // perform call with retry-on-token-early: retry once after refreshing token
                fun doCall(request: Request, attemptedRefresh: Boolean = false) {
                    httpClient.newCall(request).enqueue(object : okhttp3.Callback {
                        override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                            mainHandler.post { _gameState.value = GameScreenState.Error("submit failed: ${e.message}") }
                        }

                        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                            response.use { r ->
                                val code = r.code
                                val bodyStr = try { r.body?.string() } catch (_: Exception) { null }
                                android.util.Log.i("GameVM", "submit HTTP $code body=$bodyStr")
                                if (code == 401 && !attemptedRefresh) {
                                    // possible "Token used too early" - refresh token and retry once
                                    android.util.Log.w("GameVM", "401 received; attempting token refresh and retry")
                                    user.getIdToken(true).addOnCompleteListener { tokenTask ->
                                        if (!tokenTask.isSuccessful) {
                                            mainHandler.post { _gameState.value = GameScreenState.Error("token refresh failed: ${tokenTask.exception?.message}") }
                                            return@addOnCompleteListener
                                        }
                                        val newToken = tokenTask.result?.token ?: ""
                                        if (newToken.isBlank()) {
                                            mainHandler.post { _gameState.value = GameScreenState.Error("token refresh returned empty token") }
                                            return@addOnCompleteListener
                                        }
                                        doCall(buildRequest(newToken), attemptedRefresh = true)
                                    }
                                    return
                                }

                                if (!r.isSuccessful) {
                                    mainHandler.post { _gameState.value = GameScreenState.Error("submit HTTP ${r.code}: ${r.message}") }
                                    return
                                }
                                mainHandler.post {
                                    _gameState.value = GameScreenState.WaitingForResults
                                    listenForResults(gameId)
                                }
                            }
                        }
                    })
                }

                // initial call with current token
                val initialReq = buildRequest(idToken)
                doCall(initialReq, attemptedRefresh = false)
            }
        } catch (e: Exception) {
            _gameState.value = GameScreenState.Error("failed submitting drawing: ${e.message}")
        }
    }

    private fun observeSubmissionsAndCompute(gameId: String) {
        submissionsListener = null
    }

    fun listenForResults(gameId: String) {
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
