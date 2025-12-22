package com.example.drawmaster.presentation.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

class GameOverViewModel : ViewModel() {
    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _isCalculating = MutableStateFlow(false)
    val isCalculating: StateFlow<Boolean> = _isCalculating.asStateFlow()

    fun calculateScore(context: Context, drawingString: String?, originalString: String?) {
        if (drawingString == null || originalString == null) {
            Log.e("GameOverViewModel", "URIs nulas")
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            _isCalculating.value = true
            try {
                Log.d("GameOverViewModel", "Starting score procesing...")
                Log.d("GameOverViewModel", "Drawing URI: $drawingString")
                Log.d("GameOverViewModel", "Original URI: $originalString")

                // Loading the model
                val tfliteOptions = Interpreter.Options()
                val tflite = Interpreter(FileUtil.loadMappedFile(context, "mobilenet_v3_small.tflite"), tfliteOptions)

                // Configuring image processor
                val imageProcessor = ImageProcessor.Builder()
                    .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                    .add(NormalizeOp(127.5f, 127.5f))
                    .build()

                // Obtainging bitmaps from URIs
                val drawingBitmap = uriToBitmap(context, drawingString.toUri())
                val originalBitmap = uriToBitmap(context, originalString.toUri())
                Log.d("GameOverViewModel", "Bitmaps loaded successfully")

                // Generating embeddings
                val featDrawing = getEmbedding(tflite, drawingBitmap, imageProcessor)
                val featOriginal = getEmbedding(tflite, originalBitmap, imageProcessor)
                Log.d("GameOverViewModel", "Embeddings generados")

                // Calculating cosine similarity
                val similarity = cosineSimilarity(featDrawing, featOriginal)
                Log.d("GameOverViewModel", "Obtained cosine similarity: $similarity")

                // Scaling from 0 to 100
                var calculatedScore = (similarity.coerceIn(0f, 1f) * 100*2).toInt()
                if (calculatedScore > 100) { calculatedScore = 100}
                
                withContext(Dispatchers.Main) {
                    _score.value = calculatedScore
                    _isCalculating.value = false
                    Log.d("GameOverViewModel", "Final score: ${_score.value}")
                }

                tflite.close()
            } catch (e: Exception) {
                Log.e("GameOverViewModel", "Error processing images: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _isCalculating.value = false
                }
            }
        }
    }

    private fun getEmbedding(tflite: Interpreter, bitmap: Bitmap, processor: ImageProcessor): FloatArray {
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        val processedImage = processor.process(tensorImage)

        // MobileNetV3 Small output is 1024
        val output = Array(1) { FloatArray(1024) }
        tflite.run(processedImage.buffer, output)
        return output[0]
    }

    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in vec1.indices) {
            dotProduct += (vec1[i] * vec2[i]).toDouble()
            normA += Math.pow(vec1[i].toDouble(), 2.0)
            normB += Math.pow(vec2[i].toDouble(), 2.0)
        }
        return (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB))).toFloat()
    }

    private fun uriToBitmap(context: Context, uri: Uri): Bitmap {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        return bitmap.copy(Bitmap.Config.ARGB_8888, true)
    }

    fun navigatetoResults(navController: NavHostController) {
        navController.navigate("results_screen/${_score.value}")
    }
}