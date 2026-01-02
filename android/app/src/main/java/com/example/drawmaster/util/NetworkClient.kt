package com.example.drawmaster.util

import okhttp3.OkHttpClient

object NetworkClient {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // configure timeouts, interceptors etc here if needed
            .build()
    }
}
