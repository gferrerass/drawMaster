package com.example.drawmaster.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.drawmaster.ui.theme.DrawMasterTheme
import com.example.drawmaster.R
import com.example.drawmaster.presentation.components.CustomButton
import com.example.drawmaster.ui.theme.TealBlue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.drawmaster.presentation.viewmodels.SelectImageViewModel
import com.example.drawmaster.presentation.viewmodels.SelectImageViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectImageScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val factory = remember {
        SelectImageViewModelFactory(context.applicationContext)
    }

    val viewModel: SelectImageViewModel = viewModel(factory = factory)

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            val route = viewModel.getConfirmNavigationRoute(success)
            if (route != null) {
                navController.navigate(route)
            }
        }
    )
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val encodedUri = Uri.encode(uri.toString())
            navController.navigate("confirm_image/$encodedUri")
        }
    }

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
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Choose an image to draw",
                        color = Color.Gray,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    CustomButton(
                        name = "Take a photo",
                        description = "Use your camera",
                        image = painterResource(id = R.drawable.camera),
                        onClick = {
                            val uri = viewModel.generateTempImageUri()
                            cameraLauncher.launch(uri)
                        }
                    )
                    CustomButton(
                        name = "Upload an image",
                        description = "From your gallery",
                        image = painterResource(id = R.drawable.upload),
                        onClick = {galleryLauncher.launch("image/*")}
                    )
                    CustomButton(
                        name = "Use a sample image",
                        description = "A random picture",
                        image = painterResource(id = R.drawable.gallery),
                        onClick = {
                            val route = viewModel.generateSampleImageNavigationRoute()
                            if (route != null) {
                                navController.navigate(route)
                            }
                        }
                    )
                }
            }
        }
    )
}


@Preview(showBackground = true)
@Composable
fun SelectImageScreenPreview() {
    val navController = rememberNavController()
    DrawMasterTheme {
        SelectImageScreen(navController = navController)
    }
}