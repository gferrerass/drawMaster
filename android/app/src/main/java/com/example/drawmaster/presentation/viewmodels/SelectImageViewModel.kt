package com.example.drawmaster.presentation.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drawmaster.R
import com.example.drawmaster.domain.usecase.CreateTempImageUriUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import com.example.drawmaster.data.network.fetchUnsplashImageSelection
import com.example.drawmaster.data.network.UnsplashSelection

class SelectImageViewModel(
    private val createTempImageUriUseCase: CreateTempImageUriUseCase,
    private val applicationContext: Context
) : ViewModel() {

    private val _tempImageUri = MutableStateFlow<Uri?>(null)

    fun generateTempImageUri(): Uri {
        val uri = createTempImageUriUseCase()
        _tempImageUri.value = uri
        return uri
    }

    fun getConfirmNavigationRoute(success: Boolean): String? {
        return if (success && _tempImageUri.value != null) {
            val encodedUri = Uri.encode(_tempImageUri.value.toString())
            "confirm_image/$encodedUri"
        } else {
            null
        }
    }
    fun getConfirmNavigationRoute(success: Boolean, gameId: String? = null): String? {
        return if (success && _tempImageUri.value != null) {
            val encodedUri = Uri.encode(_tempImageUri.value.toString())
            if (gameId != null) "confirm_image/$encodedUri/$gameId" else "confirm_image/$encodedUri"
        } else {
            null
        }
    }

    suspend fun generateSampleImageNavigationRoute(gameId: String? = null): String? {
        val sampleImageResources = listOf(
            R.drawable.mountains,
            R.drawable.paris,
            R.drawable.boat,
            R.drawable.farm,
            R.drawable.beach,
            R.drawable.square
        ).distinct()

        // Defining a default local image URI
        val randomResourceId = sampleImageResources.random()
        val resourceUriString = "android.resource://" + applicationContext.packageName + "/" + randomResourceId
        var finalUriString = resourceUriString

        // Attempting to get an image through API
        val sampleImageNames = listOf(
            "animal",
           "landscape",
            "food",
            "hobby"
        ).distinct()
        val randomQuery = sampleImageNames.random()

        try {
            val selection: UnsplashSelection? = fetchUnsplashImageSelection(applicationContext, randomQuery)
            if (selection != null) {
                // prefer localUri for display; but keep remoteUrl to let Confirm screen write remote
                if (selection.localUri.isNotBlank()) finalUriString = selection.localUri
                // encode both in navigation when remoteUrl is available
                val encodedLocal = Uri.encode(finalUriString)
                return if (selection.remoteUrl != null) {
                    val encodedRemote = Uri.encode(selection.remoteUrl)
                    if (gameId != null) "confirm_image/$encodedLocal/$encodedRemote/$gameId" else "confirm_image/$encodedLocal/$encodedRemote"
                } else {
                    if (gameId != null) "confirm_image/$encodedLocal/$gameId" else "confirm_image/$encodedLocal"
                }
            }
        } catch (e: Exception) {
            // If query fails, using local image
        }
        val encodedUri = Uri.encode(finalUriString)
        return if (gameId != null) "confirm_image/$encodedUri/$gameId" else "confirm_image/$encodedUri"
    }
}

