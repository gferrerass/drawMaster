package com.example.drawmaster.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.drawmaster.ui.theme.DrawMasterTheme
import com.example.drawmaster.R
import com.example.drawmaster.presentation.components.TextButton
import com.example.drawmaster.ui.theme.LightGray
import com.example.drawmaster.ui.theme.TealBlue


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmImageScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Image(
                            painter = painterResource(id = R.drawable.arrow),
                            contentDescription = "Go Back",
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(180f)
                        )
                    }
                },
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Select Image", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TealBlue
                )
            )
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(220.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.LightGray)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.mountains),
                            contentDescription = "Selected image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextButton(
                        name = "Start drawing",
                        backgroundColor = TealBlue,
                        borderColor = TealBlue,
                        fontColor = Color.White,
                        onClick = {}
                    )
                    TextButton (
                        name = "Choose a different image",
                        backgroundColor = Color.White,
                        borderColor = LightGray,
                        fontColor = Color.Black,
                        onClick = { navController.popBackStack() }
                    )
                }
            }
        }
    )
}




@Preview(showBackground = true)
@Composable
fun ConfirmImageScreenPreview() {
    val navController = rememberNavController()
    DrawMasterTheme {
        ConfirmImageScreen(navController = navController)
    }
}