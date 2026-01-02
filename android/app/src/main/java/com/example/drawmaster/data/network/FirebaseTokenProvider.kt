package com.example.drawmaster.data.network

import com.example.drawmaster.util.FirebaseTokenProvider as UtilFirebaseTokenProvider
import kotlinx.coroutines.runBlocking

class FirebaseTokenProvider : TokenProvider {
    override fun getToken(): String? {
        return try {
            val token = runBlocking { UtilFirebaseTokenProvider.getToken(false) }
            if (token.isBlank()) null else token
        } catch (e: Exception) {
            null
        }
    }
}
