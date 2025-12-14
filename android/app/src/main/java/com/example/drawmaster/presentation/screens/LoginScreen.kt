package com.example.drawmaster.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.drawmaster.presentation.viewmodels.AuthState
import com.example.drawmaster.presentation.viewmodels.AuthViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    
    val authState by viewModel.authState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    // Si el usuario ya está autenticado, navegar a main_screen
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            navController.navigate("main_screen") {
                popUpTo("login") { inclusive = true }
            }
        }
    }
    
    // Manejar estados de autenticación
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                navController.navigate("main_screen") {
                    popUpTo("login") { inclusive = true }
                }
            }
            else -> {}
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isSignUp) "Crear Cuenta" else "Iniciar Sesión",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )
        
        if (isSignUp) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )
        }
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            singleLine = true
        )
        
        when (authState) {
            is AuthState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
            }
            is AuthState.Error -> {
                Text(
                    text = (authState as AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            else -> {}
        }
        
        Button(
            onClick = {
                if (isSignUp) {
                    viewModel.signUpWithEmail(email, password, name)
                } else {
                    viewModel.signInWithEmail(email, password)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = email.isNotBlank() && password.isNotBlank() && (if (isSignUp) name.isNotBlank() else true) && authState !is AuthState.Loading
        ) {
            Text(if (isSignUp) "Registrarse" else "Iniciar Sesión")
        }
        
        TextButton(
            onClick = { 
                isSignUp = !isSignUp
                viewModel.resetState()
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                if (isSignUp) "¿Ya tienes cuenta? Inicia sesión"
                else "¿No tienes cuenta? Regístrate"
            )
        }
    }
}
