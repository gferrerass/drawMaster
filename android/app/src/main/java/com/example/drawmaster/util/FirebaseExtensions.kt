package com.example.drawmaster.util

import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Suspend until the DatabaseReference.setValue Task completes.
 */
suspend fun DatabaseReference.setValueAwait(value: Any?) {
    suspendCancellableCoroutine<Unit> { cont ->
        try {
            this.setValue(value).addOnCompleteListener { task ->
                if (task.isSuccessful) cont.resume(Unit) else cont.resumeWithException(task.exception
                    ?: Exception("setValue failed"))
            }
        } catch (e: Exception) {
            cont.resumeWithException(e)
        }
    }
}

/**
 * Suspend until the DatabaseReference.updateChildren Task completes.
 */
suspend fun DatabaseReference.updateChildrenAwait(updates: Map<String, Any?>) {
    suspendCancellableCoroutine<Unit> { cont ->
        try {
            this.updateChildren(updates).addOnCompleteListener { task ->
                if (task.isSuccessful) cont.resume(Unit) else cont.resumeWithException(task.exception
                    ?: Exception("updateChildren failed"))
            }
        } catch (e: Exception) {
            cont.resumeWithException(e)
        }
    }
}
