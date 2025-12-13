package com.example.drawmaster.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.drawmaster.domain.repository.ImageRepository
import java.io.File

class ImageRepositoryImpl(
    private val context: Context
) : ImageRepository {

    override fun createTempImageUri(): Uri {
        val tempFile = File.createTempFile(
            "temp_image",
            ".jpg",
            context.cacheDir
        ).apply {
            createNewFile()
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }
}