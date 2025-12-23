package com.example.drawmaster.presentation.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameOverViewModel : ViewModel() {
    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _isCalculating = MutableStateFlow(false)
    val isCalculating: StateFlow<Boolean> = _isCalculating.asStateFlow()

    fun calculateScore(context: Context, drawingString: String?, originalString: String?) {
        viewModelScope.launch {
            _isCalculating.value = true
            try {
                val sc = com.example.drawmaster.presentation.scoring.ScoringUtil.computeScore(context, drawingString, originalString)
                _score.value = sc
            } catch (e: Exception) {
                Log.e("GameOverViewModel", "Error computing score: ${e.message}", e)
                _score.value = 0
            } finally {
                _isCalculating.value = false
            }
        }
    }

    fun navigatetoResults(navController: NavHostController) {
        navController.navigate("results_screen/${_score.value}")
    }
}