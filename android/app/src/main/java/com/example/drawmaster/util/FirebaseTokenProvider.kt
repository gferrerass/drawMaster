package com.example.drawmaster.util

import com.google.firebase.auth.FirebaseAuth

object FirebaseTokenProvider {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Returns a fresh ID token or an empty string on failure.
     */
    suspend fun getToken(forceRefresh: Boolean = false): String {
        val user = auth.currentUser ?: return ""
        return try {
            user.getIdTokenSuspend(forceRefresh)
        } catch (e: Exception) {
            ""
        }
    }
}
