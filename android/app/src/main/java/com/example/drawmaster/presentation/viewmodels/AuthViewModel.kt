package com.example.drawmaster.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()
    
    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _currentUser.value = firebaseAuth.currentUser
    }

    init {
        // Observing changes in authentication state
        auth.addAuthStateListener(authListener)
    }
    
    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                auth.signInWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Success("Successful Log in")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Log in error")
            }
        }
    }
    
    fun signUpWithEmail(email: String, password: String, displayName: String? = null) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                auth.createUserWithEmailAndPassword(email, password).await()
                // If name was provided, update user's profile
                val user = auth.currentUser
                if (user != null && !displayName.isNullOrBlank()) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    user.updateProfile(profileUpdates).await()
                }
                // Update currentUser & state
                _currentUser.value = auth.currentUser
                _authState.value = AuthState.Success("Account created successfully")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Create account error")
            }
        }
    }
    
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                _authState.value = AuthState.Success("Successful Google Log in")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google Log in error")
            }
        }
    }
    
    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }
    
    fun resetState() {
        _authState.value = AuthState.Idle
    }
    
    override fun onCleared() {
        super.onCleared()
        try {
            auth.removeAuthStateListener(authListener)
        } catch (_: Exception) {
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
