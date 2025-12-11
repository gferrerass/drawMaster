package com.example.drawmaster.domain.repository

import android.net.Uri

interface ImageRepository {
    fun createTempImageUri(): Uri
}