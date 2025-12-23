package com.example.drawmaster.presentation.scoring

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

object ScoringUtil {
    suspend fun computeScore(context: Context, drawingUriString: String?, originalUriString: String?): Int {
        if (drawingUriString.isNullOrBlank() || originalUriString.isNullOrBlank()) return 0
        return try {
            val drawingBitmap = uriToBitmap(context, Uri.parse(drawingUriString))
            val originalBitmap = uriToBitmap(context, Uri.parse(originalUriString))

            val tfliteOptions = Interpreter.Options()
            val tflite = Interpreter(FileUtil.loadMappedFile(context, "mobilenet_v3_small.tflite"), tfliteOptions)

            val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(127.5f, 127.5f))
                .build()

            val featDrawing = getEmbedding(tflite, drawingBitmap, imageProcessor)
            val featOriginal = getEmbedding(tflite, originalBitmap, imageProcessor)

            val similarity = cosineSimilarity(featDrawing, featOriginal)
            var calculatedScore = (similarity.coerceIn(0f, 1f) * 100 * 2).toInt()
            if (calculatedScore > 100) calculatedScore = 100
            tflite.close()
            calculatedScore
        } catch (_: Exception) {
            0
        }
    }

    private fun getEmbedding(tflite: Interpreter, bitmap: Bitmap, processor: ImageProcessor): FloatArray {
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        val processedImage = processor.process(tensorImage)
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
}
