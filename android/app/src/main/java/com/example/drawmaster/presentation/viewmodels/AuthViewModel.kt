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
        // Observar cambios en el estado de autenticación
        auth.addAuthStateListener(authListener)
    }
    
    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                auth.signInWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Success("Login exitoso")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error al iniciar sesión")
            }
        }
    }
    
    fun signUpWithEmail(email: String, password: String, displayName: String? = null) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                auth.createUserWithEmailAndPassword(email, password).await()
                // Si se proporcionó un nombre, actualizar el perfil del usuario
                val user = auth.currentUser
                if (user != null && !displayName.isNullOrBlank()) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    user.updateProfile(profileUpdates).await()
                }
                // Actualizar currentUser y estado
                _currentUser.value = auth.currentUser
                _authState.value = AuthState.Success("Cuenta creada exitosamente")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error al crear cuenta")
            }
        }
    }
    
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                _authState.value = AuthState.Success("Login con Google exitoso")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error al iniciar sesión con Google")
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
