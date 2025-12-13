package com.example.drawmaster.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.drawmaster.R
import com.example.drawmaster.presentation.components.DrawingCanvas
import com.example.drawmaster.presentation.viewmodels.GameScreenState
import com.example.drawmaster.presentation.viewmodels.GameViewModel
import com.example.drawmaster.ui.theme.TealBlue
import android.net.Uri

/**
 * GameScreen: Pantalla principal donde el usuario dibuja en single player.
 * 
 * Flujo:
 * 1. Se muestra la imagen de referencia
 * 2. El usuario tiene 30 segundos para dibujar
 * 3. Al terminar el tiempo o hacer clic en "Enviar", va a ResultScreen
 *
 * @param navController Para navegación entre pantallas
 * @param imageUriString URI de la imagen capturada que debe replicar
 * @param modifier Para personalización
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    navController: NavHostController,
    imageUriString: String?,
    modifier: Modifier = Modifier
) {
    val viewModel: GameViewModel = viewModel()
    val gameState = viewModel.gameState.collectAsState().value
    val strokes = viewModel.strokes.collectAsState().value

    // Iniciar el juego cuando la pantalla se monta
    LaunchedEffect(Unit) {
        viewModel.startGame()
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
                        Text("Draw & Match", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TealBlue
                )
            )
        },
        content = { innerPadding ->
            when (gameState) {
                is GameScreenState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is GameScreenState.Playing -> {
                    GamePlayingContent(
                        imageUriString = imageUriString,
                        timeRemaining = gameState.timeRemaining,
                        hasDrawn = gameState.hasDrawn,
                        viewModel = viewModel,
                        strokes = strokes,
                        innerPadding = innerPadding,
                        navController = navController
                    )
                }

                is GameScreenState.Finished -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Time's Up!",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealBlue
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val drawingBitmap = com.example.drawmaster.presentation.components.generateBitmapFromStrokes(
                                        strokes = strokes
                                    )
                                    // TODO: Navegar a ResultScreen con la puntuación
                                    navController.popBackStack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TealBlue)
                            ) {
                                Text("Go Back")
                            }
                        }
                    }
                }

                is GameScreenState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Error: ${gameState.message}",
                            color = Color.Red
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun GamePlayingContent(
    imageUriString: String?,
    timeRemaining: Int,
    hasDrawn: Boolean,
    viewModel: GameViewModel,
    strokes: List<com.example.drawmaster.presentation.components.DrawingStroke>,
    innerPadding: PaddingValues,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val strokeColor = viewModel.strokeColor.collectAsState().value
    val strokeWidth = viewModel.strokeWidth.collectAsState().value

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Timer prominente
        TimerDisplay(timeRemaining)

        // Imagen de referencia (pequeña, arriba)
        ReferenceImageSection(imageUriString)

        // Canvas para dibujar
        DrawingCanvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            strokeColor = Color(strokeColor),
            strokeWidth = strokeWidth,
            onDrawingChanged = { newStrokes ->
                viewModel.onDrawingChanged(newStrokes)
            }
        )

        // Controles de color y grosor
        DrawingControls(
            currentColor = strokeColor,
            currentWidth = strokeWidth,
            onColorChange = { viewModel.setStrokeColor(it) },
            onWidthChange = { viewModel.setStrokeWidth(it) }
        )

        // Controles (botones)
        GameControlButtons(
            hasDrawn = hasDrawn,
            onClear = { viewModel.clearDrawing() },
            onUndo = { viewModel.undoLastStroke() },
            onSubmit = {
                val drawingBitmap = com.example.drawmaster.presentation.components.generateBitmapFromStrokes(
                    strokes = listOf() // Aquí iría la lista de strokes
                )
                // TODO: Llamar a API para evaluar
                viewModel.finishGame(score = 85f)
            }
        )
    }
}

@Composable
private fun TimerDisplay(timeRemaining: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (timeRemaining > 10) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "⏱ $timeRemaining s",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (timeRemaining > 10) Color(0xFF2E7D32) else Color(0xFFC62828)
        )
    }
}

@Composable
private fun ReferenceImageSection(imageUriString: String?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Reference Image",
            fontSize = 12.sp,
            color = Color.Gray
        )
        
        if (imageUriString != null) {
            Image(
                painter = rememberAsyncImagePainter(imageUriString),
                contentDescription = "Reference Image",
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(100.dp)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(100.dp)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text("No image provided", color = Color.Gray)
            }
        }
    }
}

@Composable
private fun GameControlButtons(
    hasDrawn: Boolean,
    onClear: () -> Unit,
    onUndo: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onClear,
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFBCD4)
            ),
            enabled = hasDrawn
        ) {
            Text("Clear", fontSize = 12.sp, color = Color.Black)
        }

        Button(
            onClick = onUndo,
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFF9C4)
            ),
            enabled = hasDrawn
        ) {
            Text("Undo", fontSize = 12.sp, color = Color.Black)
        }

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .weight(1.2f)
                .height(40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TealBlue
            )
        ) {
            Text("Submit", fontSize = 12.sp, color = Color.White)
        }
    }
}

@Composable
private fun DrawingControls(
    currentColor: Int,
    currentWidth: Float,
    onColorChange: (Int) -> Unit,
    onWidthChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selector de colores
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val colors = listOf(
                android.graphics.Color.BLACK,
                android.graphics.Color.RED,
                android.graphics.Color.BLUE,
                android.graphics.Color.GREEN,
                android.graphics.Color.rgb(255, 165, 0),
                android.graphics.Color.rgb(128, 0, 128)
            )
            
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = Color(color),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                        .then(
                            if (currentColor == color) {
                                Modifier.border(
                                    width = 2.dp,
                                    color = TealBlue,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                            } else Modifier
                        )
                        .clickable { onColorChange(color) }
                )
            }
        }

        // Slider de grosor (más compacto)
        Column(
            modifier = Modifier.width(120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${currentWidth.toInt()}px",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
            
            Slider(
                value = currentWidth,
                onValueChange = onWidthChange,
                valueRange = 1f..10f,
                steps = 8,
                modifier = Modifier.height(20.dp),
                colors = SliderDefaults.colors(
                    thumbColor = TealBlue,
                    activeTrackColor = TealBlue
                )
            )
        }
    }
}
