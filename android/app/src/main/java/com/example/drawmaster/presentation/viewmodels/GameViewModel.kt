package com.example.drawmaster.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drawmaster.presentation.components.DrawingStroke
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estados posibles durante el flujo de un juego single player.
 */
sealed class GameScreenState {
    object Loading : GameScreenState()
    data class Playing(
        val timeRemaining: Int,
        val hasDrawn: Boolean = false
    ) : GameScreenState()
    data class Finished(
        val score: Float,
        val isHighScore: Boolean = false
    ) : GameScreenState()
    data class Error(val message: String) : GameScreenState()
}

/**
 * ViewModel para GameScreen (single player).
 * Gestiona:
 * - Tiempo límite del juego (30 segundos)
 * - Estado de los strokes dibujados
 * - Transiciones entre states
 */
class GameViewModel : ViewModel() {

    private val _gameState = MutableStateFlow<GameScreenState>(GameScreenState.Loading)
    val gameState: StateFlow<GameScreenState> = _gameState.asStateFlow()

    private val _strokes = MutableStateFlow<List<DrawingStroke>>(emptyList())
    val strokes: StateFlow<List<DrawingStroke>> = _strokes.asStateFlow()

    private val _imageName = MutableStateFlow<String?>(null)
    val imageName: StateFlow<String?> = _imageName.asStateFlow()

    private var timerJob: Job? = null
    private val totalGameTime = 30 // segundos

    fun startGame(imageName: String = "drawing_challenge") {
        _imageName.value = imageName
        _strokes.value = emptyList()
        startTimer()
    }

    fun onDrawingChanged(newStrokes: List<DrawingStroke>) {
        _strokes.value = newStrokes
        
        // Actualizar estado si es la primera vez que dibuja
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

    fun finishGame(score: Float = 0f) {
        timerJob?.cancel()
        _gameState.value = GameScreenState.Finished(
            score = score,
            isHighScore = false // TODO: Implementar lógica de high scores
        )
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
            // Tiempo se acabó
            finishGame(score = 0f)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
