package com.example.drawmaster.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.remember
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
import coil.compose.rememberAsyncImagePainter
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drawmaster.presentation.viewmodels.ConfirmImageViewModel
import com.example.drawmaster.presentation.viewmodels.ConfirmImageViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmImageScreen(
    navController: NavHostController,
    imageUriString: String?,
    modifier: Modifier = Modifier
) {
    val factory = remember {
        ConfirmImageViewModelFactory(imageUriString)
    }
    val viewModel: ConfirmImageViewModel = viewModel(factory = factory)
    val uiState = viewModel.uiState.collectAsState().value
    val imageUri = uiState.imageUri

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Select Image", color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow),
                            contentDescription = "Go Back",
                            modifier = Modifier.size(24.dp).rotate(180f),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        if (imageUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(model = imageUri),
                                contentDescription = "Selected image from camera",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            // Displaying default image if there is no Uri
                            Image(
                                painter = painterResource(id = R.drawable.mountains),
                                contentDescription = "Placeholder image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
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
                        onClick = { viewModel.onStartDrawingClicked(navController) }
                    )
                    TextButton (
                        name = "Choose a different image",
                        backgroundColor = Color.White,
                        borderColor = LightGray,
                        fontColor = Color.Black,
                        onClick = { viewModel.onChooseDifferentImageClicked(navController) }
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
        ConfirmImageScreen(navController = navController, imageUriString = null)
    }
}