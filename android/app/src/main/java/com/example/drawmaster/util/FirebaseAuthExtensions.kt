package com.example.drawmaster.util

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun FirebaseUser.getIdTokenSuspend(forceRefresh: Boolean = false): String =
    suspendCancellableCoroutine { cont ->
        this.getIdToken(forceRefresh).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                cont.resumeWithException(task.exception ?: Exception("getIdToken failed"))
                return@addOnCompleteListener
            }
            val token = task.result?.token
            if (token == null) cont.resumeWithException(Exception("idToken null")) else cont.resume(token)
        }
    }
