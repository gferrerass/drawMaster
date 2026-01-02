package com.example.drawmaster.util

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okhttp3.Request
import okhttp3.OkHttpClient
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Suspendable, cancellable wrapper around OkHttp Call. Returns the Response; caller must close it.
 */
suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (cont.isActive) cont.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            if (cont.isActive) cont.resume(response) else response.close()
        }
    })

    cont.invokeOnCancellation {
        try { cancel() } catch (_: Exception) {}
    }
}

/**
 * Alternative helper that accepts a client and request and returns a cancellable Response.
 */
suspend fun awaitResponse(client: OkHttpClient, request: Request): Response = suspendCancellableCoroutine { cont ->
    val call = client.newCall(request)
    call.enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (cont.isActive) cont.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            if (cont.isActive) cont.resume(response) else response.close()
        }
    })

    cont.invokeOnCancellation {
        try { call.cancel() } catch (_: Exception) {}
    }
}
