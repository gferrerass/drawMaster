package com.example.drawmaster.data.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class UnsplashApiResponse(
    @JsonProperty("results") val results: List<UnsplashImage>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UnsplashImage(
    @JsonProperty("id") val id: String,
    @JsonProperty("urls") val urls: UnsplashUrls
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UnsplashUrls(
    @JsonProperty("raw") val raw: String,
    @JsonProperty("full") val full: String,
    @JsonProperty("regular") val regular: String,
    @JsonProperty("small") val small: String,
    @JsonProperty("thumb") val thumb: String
)

data class UnsplashSelection(
    val localUri: String,
    val remoteUrl: String?
)

suspend fun downloadAndSaveUnsplashImage(context: Context, imageUrl: String): File? {
    return withContext(Dispatchers.IO) {
        try {
            // Downloading image from URL
            val connection = URL(imageUrl).openConnection()
            connection.doInput = true
            connection.connect()
            val inputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) {
                Log.e("UNSPLASH_DOWNLOAD", "Failed to decode bitmap from URL")
                return@withContext null
            }

            // Creating the temporary file
            val filename = "temp_unsplash_${UUID.randomUUID()}.jpg"
            val file = File(context.cacheDir, filename)

            // Saving the bitmap to the file
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
            bitmap.recycle()

            Log.d("UNSPLASH_DOWNLOAD", "Image saved to: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e("UNSPLASH_DOWNLOAD", "Error downloading image: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}

suspend fun fetchUnsplashImageSelection(context: Context, query: String): UnsplashSelection? {
    val tag = "UNSPLASH_DEBUG"
    val accessKey = "d_3tFqV7FUA2pehZpZWjqRZo1dSvOtPJOLcU34-V83U"
    val randomPage = (1..50).random()
    val apiUrl = "https://api.unsplash.com/search/photos?" +
            "query=$query" +
            "&client_id=$accessKey" +
            "&page=$randomPage" +
            "&per_page=10" + // 10 results per page
            "&sig=${System.currentTimeMillis()}"

    return withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Starting query for: $query")

            // HTTP query
            val jsonResponse = URL(apiUrl).readText()
            Log.d(tag, "JSON received successfully: ${jsonResponse.take(100)}...")

            // Mapping using Jackson
            val mapper = jacksonObjectMapper()
            val response: UnsplashApiResponse = mapper.readValue(jsonResponse)

            // Obtaining random URL
            val imageUrl = response.results.shuffled().firstOrNull()?.urls?.small
            if (imageUrl != null) {
                Log.i(tag, "Found image! URL: $imageUrl")
                
                // Downloading and saving the image locally
                val localFile = downloadAndSaveUnsplashImage(context, imageUrl)
                if (localFile != null) {
                    val localUri = Uri.fromFile(localFile).toString()
                    Log.i(tag, "Image saved locally. URI: $localUri")
                    UnsplashSelection(localUri = localUri, remoteUrl = imageUrl)
                } else {
                    Log.w(tag, "Failed to download and save image locally")
                    UnsplashSelection(localUri = "", remoteUrl = imageUrl)
                }
            } else {
                Log.w(tag, "No results found")
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Query error: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}