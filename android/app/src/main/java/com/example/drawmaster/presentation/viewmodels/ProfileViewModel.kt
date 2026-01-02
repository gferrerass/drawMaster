package com.example.drawmaster.presentation.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed class LocationUiState {
    object Idle : LocationUiState()
    object Loading : LocationUiState()
    data class Success(val latitude: Double, val longitude: Double, val accuracy: Float?, val timestamp: Long?) : LocationUiState()
    data class Error(val message: String) : LocationUiState()
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(application)

    private val _uiState = MutableStateFlow<LocationUiState>(LocationUiState.Idle)
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    fun refreshLocation() {
        viewModelScope.launch {
            _uiState.value = LocationUiState.Loading
            try {
                val loc = getCurrentLocation()
                if (loc != null) {
                    _uiState.value = LocationUiState.Success(loc.latitude, loc.longitude, if (loc.hasAccuracy()) loc.accuracy else null, loc.time)
                } else {
                    _uiState.value = LocationUiState.Error("Couldn't obtain location")
                }
            } catch (e: SecurityException) {
                _uiState.value = LocationUiState.Error("Missing location permission")
            } catch (e: Exception) {
                _uiState.value = LocationUiState.Error(e.localizedMessage ?: "Unknown Error")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { cont ->
        try {
            val task = fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            task.addOnSuccessListener { location ->
                cont.resume(location)
            }
            task.addOnFailureListener {
                cont.resume(null)
            }
            cont.invokeOnCancellation {
                // No-op: Task from FusedLocationProviderClient does not expose a cancel() method
            }
        } catch (ex: Exception) {
            cont.resume(null)
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
