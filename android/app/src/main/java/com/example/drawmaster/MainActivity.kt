package com.example.drawmaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.drawmaster.presentation.DrawMasterNavHost
import com.example.drawmaster.ui.theme.DrawMasterTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DrawMasterTheme {
                val navController = rememberNavController()
                DrawMasterNavHost(
                    navController = navController,
                    modifier = Modifier.fillMaxSize())
            }
        }
        FirebaseApp.initializeApp(this)

    }
}