package com.example.drawmaster.presentation.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.drawmaster.R
import com.example.drawmaster.domain.usecase.CreateTempImageUriUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.core.net.toUri

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

    fun generateSampleImageNavigationRoute(): String? {
        val sampleImageResources = listOf(
            R.drawable.mountains,
            R.drawable.paris,
            R.drawable.boat,
            R.drawable.farm,
            R.drawable.beach
        ).distinct()

        if (sampleImageResources.isEmpty()) return null

        val randomResourceId = sampleImageResources.random()
        val resourceUriString = "android.resource://" + applicationContext.packageName + "/" + randomResourceId
        val uri = resourceUriString.toUri()
        val encodedUri = Uri.encode(uri.toString())

        return "confirm_image/$encodedUri"
    }
}

