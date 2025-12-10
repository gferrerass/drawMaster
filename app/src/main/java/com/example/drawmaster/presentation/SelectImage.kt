package com.example.drawmaster.presentation

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.drawmaster.ui.theme.DrawMasterTheme
import com.example.drawmaster.R
import com.example.drawmaster.presentation.components.CustomButton
import com.example.drawmaster.ui.theme.TealBlue
import java.io.File

fun getTempImageUri(context: Context): Uri {
    val tempFile = File.createTempFile(
        "temp_image",
        ".jpg",
        context.cacheDir
    ).apply {
        createNewFile()
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectImageScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onMultiplayer: () -> Unit = {}
) {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && imageUri != null) {
                // If the picture was taken successfully, navigating to the confirmation screen
                val encodedUri = Uri.encode(imageUri.toString())
                // Including encoded image Uri
                navController.navigate("confirm_image/$encodedUri")
            }
        }
    )
    
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
                        Text("Select Image",  color = Color.White)
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
                            val uri = getTempImageUri(context)
                            imageUri = uri
                            // Launching the camera intent
                            cameraLauncher.launch(uri)
                        }
                    )
                    CustomButton(
                        name = "Upload an image",
                        description = "From your gallery",
                        image = painterResource(id = R.drawable.upload),
                        onClick = onMultiplayer
                    )
                    CustomButton(
                        name = "Use a sample image",
                        description = "A random picture",
                        image = painterResource(id = R.drawable.gallery),
                        onClick = onMultiplayer
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
