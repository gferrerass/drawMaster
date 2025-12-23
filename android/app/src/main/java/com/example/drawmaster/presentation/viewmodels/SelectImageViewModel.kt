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
import com.example.drawmaster.data.network.fetchUnsplashImageUrl

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

    suspend fun generateSampleImageNavigationRoute(): String? {
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
            val localUri = fetchUnsplashImageUrl(applicationContext, randomQuery)
            if (localUri != null) {
                finalUriString = localUri
            }
        } catch (e: Exception) {
            // If query fails, using local image
        }
        val encodedUri = Uri.encode(finalUriString)
        return "confirm_image/$encodedUri"
    }
}

