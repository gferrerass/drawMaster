package com.example.drawmaster.presentation.components

import android.content.Context
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.border
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
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
@param onSizeChanged Callback when canvas size changes (width, height in pixels)
 */
@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    strokes: List<DrawingStroke> = emptyList(),
    strokeColor: Color = Color.Black,
    strokeWidth: Float = 3f,
    onDrawingChanged: (List<DrawingStroke>) -> Unit = {},
    onSizeChanged: (Int, Int) -> Unit = { _, _ -> }
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
            .onSizeChanged { size ->
                onSizeChanged(size.width, size.height)
            }
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
@param width in pixels (should match canvas width)
@param height in pixels (should match canvas height)
@return Bitmap with exact canvas dimensions
 */
fun generateBitmapFromStrokes(
    strokes: List<DrawingStroke>,
    width: Int,
    height: Int,
): Bitmap? {
    if (width <= 0 || height <= 0) return null
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    // Painting the background with solid white
    canvas.drawColor(android.graphics.Color.WHITE)

    strokes.forEach { stroke ->
        val paint = android.graphics.Paint().apply {
            color = stroke.color
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = stroke.width
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            isAntiAlias = true
        }
        if (stroke.points.size > 1) {
            for (i in 1 until stroke.points.size) {
                val prevPoint = stroke.points[i - 1]
                val currentPoint = stroke.points[i]
                canvas.drawLine(
                    prevPoint.x,
                    prevPoint.y,
                    currentPoint.x,
                    currentPoint.y,
                    paint
                )
            }
        }
    }
    return bitmap
}


fun saveBitmapToTempJpeg(context: Context, bitmap: Bitmap): File? {
    // Creating temporary image file
    val filename = "temp_drawing_${UUID.randomUUID()}.jpg"
    val file = File(context.cacheDir, filename)

    return try {
        val out = FileOutputStream(file)
        // Compressing image to JPEG format
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        out.flush()
        out.close()
        // Returning image file
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun returnDrawing(context: Context, strokes: List<DrawingStroke>, canvasWidth: Int, canvasHeight: Int): String? {
    // Generating bitmap with exact canvas dimensions
    val bitmap = generateBitmapFromStrokes(strokes, canvasWidth, canvasHeight) ?: return null

    // Saving file
    val file = try {
        saveBitmapToTempJpeg(context, bitmap)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    // Returning URI as a String
    return file?.let {
        val uri = Uri.fromFile(it)
        uri.toString()
    }
}