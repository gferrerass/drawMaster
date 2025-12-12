package com.example.drawmaster.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.drawmaster.domain.usecase.CreateTempImageUriUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SelectImageViewModel(
    private val createTempImageUriUseCase: CreateTempImageUriUseCase
) : ViewModel() {

    private val _tempImageUri = MutableStateFlow<Uri?>(null)
    val tempImageUri: StateFlow<Uri?> = _tempImageUri.asStateFlow()

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
}

