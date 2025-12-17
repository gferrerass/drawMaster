package com.example.drawmaster.data.network

import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Tasks

class FirebaseTokenProvider : TokenProvider {
    override fun getToken(): String? {
        return try {
            val user = FirebaseAuth.getInstance().currentUser ?: return null
            val task = user.getIdToken(false)
            val result = Tasks.await(task)
            result?.token
        } catch (e: Exception) {
            null
        }
    }
}
