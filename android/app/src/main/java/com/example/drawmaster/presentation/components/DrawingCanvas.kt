package com.example.drawmaster.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import androidx.compose.foundation.border
import android.graphics.Canvas as AndroidCanvas

data class DrawingPoint(
    val x: Float,
    val y: Float
)

data class DrawingStroke(
    val points: List<DrawingPoint>,
    val color: Int = android.graphics.Color.BLACK,
    val width: Float = 3f
)

/**
@param modifier Modifier to customise size
@param strokeColor Black by default
@param strokeWidth
@param onDrawingChanged Callback when user has drawn something
 */
@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    strokes: List<DrawingStroke> = emptyList(),
    strokeColor: Color = Color.Black,
    strokeWidth: Float = 3f,
    onDrawingChanged: (List<DrawingStroke>) -> Unit = {}
) {
    var strokesState by remember { mutableStateOf<List<DrawingStroke>>(emptyList()) }
    val currentPath = remember { mutableStateOf<List<DrawingPoint>>(emptyList()) }

    LaunchedEffect(strokes) {
        strokesState = strokes
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(Color.White)
            .border(1.dp, Color.LightGray)
            .clipToBounds()
            .pointerInput(strokeColor, strokeWidth) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentPath.value = listOf(DrawingPoint(offset.x, offset.y))
                    },
                    onDrag = { change, _ ->
                        val newPoints = currentPath.value.toMutableList()
                        newPoints.add(DrawingPoint(change.position.x, change.position.y))
                        currentPath.value = newPoints
                        change.consume()
                    },
                    onDragEnd = {
                        if (currentPath.value.isNotEmpty()) {
                            val newStrokes = strokesState.toMutableList()
                            newStrokes.add(DrawingStroke(
                                points = currentPath.value,
                                color = strokeColor.toArgb(),
                                width = strokeWidth
                            ))
                            strokesState = newStrokes
                            onDrawingChanged(newStrokes)
                            currentPath.value = emptyList()
                        }
                    }
                )
            }
            .drawBehind {
                // Drawing finished strokes
                strokesState.forEach { stroke ->
                    if (stroke.points.size >= 2) {
                        for (i in 1 until stroke.points.size) {
                            val prevPoint = stroke.points[i - 1]
                            val currentPoint = stroke.points[i]
                            
                            drawLine(
                                color = Color(stroke.color),
                                start = androidx.compose.ui.geometry.Offset(prevPoint.x, prevPoint.y),
                                end = androidx.compose.ui.geometry.Offset(currentPoint.x, currentPoint.y),
                                strokeWidth = stroke.width,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                }
                
                // Drawing current strokes
                if (currentPath.value.size >= 2) {
                    for (i in 1 until currentPath.value.size) {
                        val prevPoint = currentPath.value[i - 1]
                        val currentPoint = currentPath.value[i]
                        
                        drawLine(
                            color = strokeColor,
                            start = androidx.compose.ui.geometry.Offset(prevPoint.x, prevPoint.y),
                            end = androidx.compose.ui.geometry.Offset(currentPoint.x, currentPoint.y),
                            strokeWidth = strokeWidth,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
            }
    )
}

/**
@param strokes Array of drawn strokes
@param width in pixels
@param height in pixels
@param strokeColor
@param strokeWidth
@return Bitmap
 */
fun generateBitmapFromStrokes(
    strokes: List<DrawingStroke>,
    width: Int = 512,
    height: Int = 512,
    strokeColor: Int = android.graphics.Color.BLACK,
    strokeWidth: Float = 3f
): Bitmap? {
    if (strokes.isEmpty()) return null

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    // Filling up background with solid white colour
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = android.graphics.Paint().apply {
        color = strokeColor
        style = android.graphics.Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        isAntiAlias = true
    }

    // Scaling points if necessary (they come in composable coordinates)
    val canvasWidth = 400 // DrawingCanvas is 400.dp wide
    val scaleX = width.toFloat() / canvasWidth
    val scaleY = height.toFloat() / 400

    strokes.forEach { stroke ->
        if (stroke.points.size > 1) {
            for (i in 1 until stroke.points.size) {
                val prevPoint = stroke.points[i - 1]
                val currentPoint = stroke.points[i]

                canvas.drawLine(
                    prevPoint.x * scaleX,
                    prevPoint.y * scaleY,
                    currentPoint.x * scaleX,
                    currentPoint.y * scaleY,
                    paint
                )
            }
        }
    }
    return bitmap
}
