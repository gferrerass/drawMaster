package com.example.drawmaster.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ConfirmImageViewModelFactory(
    private val imageUriString: String?
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConfirmImageViewModel::class.java)) {
            return ConfirmImageViewModel(imageUriString) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}