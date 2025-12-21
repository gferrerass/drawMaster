package com.example.drawmaster.presentation.screens

import android.graphics.drawable.Icon
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.drawmaster.R
import com.example.drawmaster.presentation.components.TextButton
import com.example.drawmaster.ui.theme.DrawMasterTheme
import com.example.drawmaster.ui.theme.TealBlue
import java.util.Objects.toString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    navController: NavHostController,
    score: Int?,
    modifier: Modifier = Modifier
) {
    val finalScore = score ?: 0
    // Calculamos el progreso (0.0 a 1.0). Ejemplo: si el max es 100
    val progress = finalScore / 100f

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Results", color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = TealBlue)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    name = "Back to Menu",
                    backgroundColor = TealBlue,
                    borderColor = TealBlue,
                    fontColor = Color.White,
                    onClick = { navController.navigate("main_screen") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.sparkle),
                contentDescription = "Sparkle",
                modifier = Modifier.size(80.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Keep Practicing!",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1D2136),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your score",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = finalScore.toString(),
                color = TealBlue,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape),
                color = TealBlue,
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ResultsScreenPreview() {
    val navController = rememberNavController()
    DrawMasterTheme {
        ResultsScreen(navController = navController, score = 50)
    }
}