package com.example.drawmaster.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // For Android emulator, host machine localhost is 10.0.2.2
    private const val BASE_URL = "http://10.0.2.2:5000/"

    private val logging = HttpLoggingInterceptor().apply {
        // BODY can attempt to read response bodies and may fail if the server
        // closes the connection early. Use BASIC to avoid reading bodies here.
        level = HttpLoggingInterceptor.Level.BASIC
    }

    /** Create a Retrofit instance that injects Authorization header using the provided TokenProvider. */
    fun createRetrofit(tokenProvider: TokenProvider): Retrofit {
        val authInterceptor = AuthInterceptor(tokenProvider)
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
