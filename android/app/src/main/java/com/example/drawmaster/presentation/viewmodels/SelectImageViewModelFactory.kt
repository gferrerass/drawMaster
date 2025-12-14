package com.example.drawmaster.presentation.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.drawmaster.data.repository.ImageRepositoryImpl
import com.example.drawmaster.domain.usecase.CreateTempImageUriUseCase

class SelectImageViewModelFactory(
    private val applicationContext: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SelectImageViewModel::class.java)) {

            val repository = ImageRepositoryImpl(applicationContext)

            val useCase = CreateTempImageUriUseCase(repository)


            return SelectImageViewModel(useCase, applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

