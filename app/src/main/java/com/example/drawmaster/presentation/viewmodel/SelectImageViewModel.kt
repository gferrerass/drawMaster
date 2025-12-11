package com.example.drawmaster.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.drawmaster.domain.usecase.CreateTempImageUriUseCase

class SelectImageViewModel(
    private val createTempImageUriUseCase: CreateTempImageUriUseCase
) : ViewModel() {

    var tempImageUri: Uri? = null
        private set

    fun generateTempImageUri(): Uri {
        val uri = createTempImageUriUseCase()
        tempImageUri = uri
        return uri
    }

    fun getConfirmNavigationRoute(success: Boolean): String? {
        return if (success && tempImageUri != null) {
            val encodedUri = Uri.encode(tempImageUri.toString())
            "confirm_image/$encodedUri"
        } else {
            null
        }
    }
}

