package com.example.drawmaster.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.drawmaster.R
import com.example.drawmaster.presentation.components.TextButton
import com.example.drawmaster.presentation.viewmodels.GameOverViewModel

import com.example.drawmaster.ui.theme.DrawMasterTheme
import com.example.drawmaster.ui.theme.TealBlue


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameOverScreen(
    navController: NavHostController,
    drawingUriString: String?,
    originalUriString: String?,
    modifier: Modifier = Modifier
) {
    val viewModel: GameOverViewModel = viewModel()
    val context = LocalContext.current
    viewModel.calculateScore(context, drawingUriString, originalUriString)
    // The user can't go back on this screen
    BackHandler(enabled = true) {
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Game Over", color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = TealBlue)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(150.dp))
            Text(
                text = "Your final drawing:",
                color = Color.Gray,
                style = MaterialTheme.typography.headlineSmall
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = androidx.compose.ui.graphics.RectangleShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    val painter = if (drawingUriString != null) {
                        rememberAsyncImagePainter(model = drawingUriString)
                    } else {
                        painterResource(id = R.drawable.mountains)
                    }
                    Image(
                        painter = painter,
                        contentDescription = "Drawn picture",
                        modifier = Modifier.wrapContentSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                name = "See results",
                backgroundColor = TealBlue,
                borderColor = TealBlue,
                fontColor = Color.White,
                onClick = {
                    viewModel.navigatetoResults(navController)
                    }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameOverScreenPreview() {
    val navController = rememberNavController()
    DrawMasterTheme {
        GameOverScreen(navController = navController, drawingUriString = null, originalUriString = null)
    }
}