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

// 3 possible states: Loading, Playing and Finished
sealed class GameScreenState {
    object Loading : GameScreenState()
    data class Playing(
        val timeRemaining: Int,
        val hasDrawn: Boolean = false
    ) : GameScreenState()
    object Finished : GameScreenState()
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

    fun startGame(imageName: String = "drawing_challenge") {
        _imageName.value = imageName
        _strokes.value = emptyList()
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
        _gameState.value = GameScreenState.Finished
    }

    fun navigatetoGameOverScreen(navController: NavHostController, drawingURI: String, originalURI: String) {
        val encodedUri = Uri.encode(drawingURI)
        val encodedOriginalUri = Uri.encode(originalURI)
        navController.navigate("game_over_screen/$encodedUri/$encodedOriginalUri")
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
    }
}
