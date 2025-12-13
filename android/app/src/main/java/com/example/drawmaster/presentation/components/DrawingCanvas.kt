package com.example.drawmaster.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas

data class DrawingPoint(
    val x: Float,
    val y: Float
)

data class DrawingStroke(
    val points: List<DrawingPoint>
)

/**
 * DrawingCanvas composable que permite dibujar con gestos táctiles.
 * Captura los puntos dibujados y puede generar un Bitmap del dibujo.
 *
 * @param modifier Modifier para personalizar el tamaño y estilo
 * @param strokeColor Color del trazo (por defecto negro)
 * @param strokeWidth Grosor del trazo en dp
 * @param onDrawingChanged Callback cuando el usuario dibuja algo
 */
@Composable
fun DrawingCanvas(
    modifier: Modifier = Modifier,
    strokeColor: Color = Color.Black,
    strokeWidth: Float = 3f,
    onDrawingChanged: (List<DrawingStroke>) -> Unit = {}
) {
    val strokes = remember { mutableStateOf<List<DrawingStroke>>(emptyList()) }
    val currentPath = remember { mutableStateOf<List<DrawingPoint>>(emptyList()) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .background(Color.White)
            .clipToBounds()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // offset es la posición inicial del toque
                        currentPath.value = listOf(DrawingPoint(offset.x, offset.y))
                    },
                    onDrag = { change, _ ->
                        // change.position es la posición ABSOLUTA del dedo
                        val newPoints = currentPath.value.toMutableList()
                        newPoints.add(DrawingPoint(change.position.x, change.position.y))
                        currentPath.value = newPoints
                        change.consume()
                    },
                    onDragEnd = {
                        if (currentPath.value.isNotEmpty()) {
                            val newStrokes = strokes.value.toMutableList()
                            newStrokes.add(DrawingStroke(currentPath.value))
                            strokes.value = newStrokes
                            onDrawingChanged(newStrokes)
                            currentPath.value = emptyList()
                        }
                    }
                )
            }
            .drawBehind {
                // Usar size.width y size.height para asegurar que coincida
                // drawRect ya dibuja en el área disponible
                
                // Dibujar strokes completados
                strokes.value.forEach { stroke ->
                    if (stroke.points.size >= 2) {
                        for (i in 1 until stroke.points.size) {
                            val prevPoint = stroke.points[i - 1]
                            val currentPoint = stroke.points[i]
                            
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
                
                // Dibujar trazo actual mientras se dibuja
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
 * Función para generar un Bitmap a partir de los strokes dibujados.
 * Útil para enviar el dibujo a una API de evaluación.
 *
 * @param strokes Lista de strokes dibujados
 * @param width Ancho del bitmap en píxeles
 * @param height Alto del bitmap en píxeles
 * @param strokeColor Color del trazo (como Int)
 * @param strokeWidth Grosor del trazo
 * @return Bitmap del dibujo, o null si no hay strokes
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

    // Llenar fondo de blanco
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = android.graphics.Paint().apply {
        color = strokeColor
        style = android.graphics.Paint.Style.STROKE
        this.strokeWidth = strokeWidth
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
        isAntiAlias = true
    }

    // Escalar puntos si es necesario (los puntos están en coordenadas de composable)
    val canvasWidth = 400 // El DrawingCanvas tiene 400.dp de ancho
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
