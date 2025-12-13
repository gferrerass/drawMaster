package com.example.drawmaster.domain.usecase

import com.example.drawmaster.domain.repository.ImageRepository

class CreateTempImageUriUseCase(
    private val repository: ImageRepository
) {
    operator fun invoke() = repository.createTempImageUri()
}