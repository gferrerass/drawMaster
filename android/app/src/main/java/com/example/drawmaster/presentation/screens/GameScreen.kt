package com.example.drawmaster.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.drawmaster.presentation.components.DrawingCanvas
import com.example.drawmaster.presentation.components.ShakeDetector
import com.example.drawmaster.presentation.viewmodels.GameScreenState
import com.example.drawmaster.presentation.viewmodels.GameViewModel
import com.example.drawmaster.ui.theme.TealBlue
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.drawmaster.R
import com.example.drawmaster.ui.theme.DrawMasterTheme
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import com.example.drawmaster.presentation.components.returnDrawing
import com.example.drawmaster.presentation.scoring.ScoringUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    navController: NavHostController,
    imageUriString: String?,
    gameId: String? = null,
    modifier: Modifier = Modifier
) {
    val viewModel: GameViewModel = viewModel()
    val gameState = viewModel.gameState.collectAsState().value
    val strokes = viewModel.strokes.collectAsState().value
    val canvasWidth = viewModel.canvasWidth.collectAsState().value
    val canvasHeight = viewModel.canvasHeight.collectAsState().value
    val context = LocalContext.current
    val submittedMultiplayer = remember { androidx.compose.runtime.mutableStateOf(false) }

    // Initialising ShakeDetector
    val shakeDetector = remember {
        ShakeDetector(context) {
            viewModel.clearDrawing()
            Toast.makeText(context, "Shake detected! Erasing canvas", Toast.LENGTH_SHORT).show()
        }
    }

    // The user can't go back on this screen
    BackHandler(enabled = true) {
    }

    LaunchedEffect(Unit) {
        viewModel.startGame(gameId = gameId)
    }

    // Activating/Deactivating shakeDetector when the screen is displayed/hidden
    DisposableEffect(Unit) {
        shakeDetector.start()
        onDispose {
            shakeDetector.stop()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Draw & Match", color = Color.White)
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
                    // Ensuring that canvas dimensions are available before generating the bitmap
                    if (canvasWidth > 0 && canvasHeight > 0) {
                        val drawingFileURI = returnDrawing(context, strokes, canvasWidth, canvasHeight)
                            if (drawingFileURI != null && imageUriString != null) {
                                if (gameId == null) {
                                    viewModel.navigatetoGameOverScreen(navController, drawingFileURI, imageUriString!!)
                                } else {
                                    // multiplayer: compute local score and submit with score
                                    if (!submittedMultiplayer.value) {
                                        LaunchedEffect(drawingFileURI) {
                                            val sc = com.example.drawmaster.presentation.scoring.ScoringUtil.computeScore(context, drawingFileURI, imageUriString)
                                            viewModel.submitMultiplayerDrawingForGame(gameId, drawingFileURI, imageUriString!!, sc)
                                            submittedMultiplayer.value = true
                                        }
                                    }
                                }
                            }
                    }
                }
                    is GameScreenState.WaitingForResults -> {
                        // show waiting UI while other player submits or timer expires
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Waiting for opponent to finish...")
                            }
                        }
                        // if results are available, navigate
                        val results = viewModel.results.collectAsState().value
                            if (results != null) {
                            // results are available server-side (scores only). Navigate to GameOver and pass gameId.
                            viewModel.navigatetoGameOverScreen(navController, "", "", gameId)
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

    val configuration = LocalConfiguration.current
    val screenH = configuration.screenHeightDp
    // consider small screens under 700dp height (tweakable)
    val isSmallScreen = screenH < 700

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        TimerDisplay(timeRemaining, isSmallScreen)
        ReferenceImageSection(imageUriString, isSmallScreen)
        DrawingCanvas(
            modifier = Modifier
                .fillMaxWidth()
                // give the canvas more weight on small screens so controls remain visible
                .weight(if (isSmallScreen) 1.4f else 1f),
            strokes = strokes,
            strokeColor = Color(strokeColor),
            strokeWidth = strokeWidth,
            onDrawingChanged = { newStrokes ->
                viewModel.onDrawingChanged(newStrokes)
            },
            onSizeChanged = { width, height ->
                viewModel.setCanvasSize(width, height)
            }
        )

        // Color and width stroke controls
        DrawingControls(
            currentColor = strokeColor,
            currentWidth = strokeWidth,
            onColorChange = { viewModel.setStrokeColor(it) },
            onWidthChange = { viewModel.setStrokeWidth(it) }
        )

        // Lower screen buttons
        GameControlButtons(
            hasDrawn = hasDrawn,
            onClear = { viewModel.clearDrawing() },
            onUndo = { viewModel.undoLastStroke() },
            onSubmit = {
                viewModel.finishGame()
            }
        )
    }
}

@Composable
private fun TimerDisplay(timeRemaining: Int, isSmall: Boolean = false) {
    val padding = if (isSmall) 8.dp else 16.dp
    val font = if (isSmall) 18.sp else 24.sp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (timeRemaining > 10) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "⏱ $timeRemaining s",
            fontSize = font,
            fontWeight = FontWeight.Bold,
            color = if (timeRemaining > 10) Color(0xFF2E7D32) else Color(0xFFC62828)
        )
    }
}

@Composable
private fun ReferenceImageSection(imageUriString: String?, isSmall: Boolean = false) {
    val cardHeight = if (isSmall) 120.dp else 200.dp
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Reference Image",
            fontSize = if (isSmall) 10.sp else 12.sp,
            color = Color.Gray
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.LightGray),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            if (imageUriString != null) {
                Image(
                    painter = rememberAsyncImagePainter(imageUriString),
                    contentDescription = "Reference Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.mountains),
                    contentDescription = "Placeholder Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
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
        // Color selector
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

        // Width selector slider
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
                valueRange = 5f..20f,
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

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    val navController = rememberNavController()
    DrawMasterTheme {
        GameScreen(navController = navController, imageUriString = null)
    }
}