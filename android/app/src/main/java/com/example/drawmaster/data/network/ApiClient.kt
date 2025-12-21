package com.example.drawmaster.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.drawmaster.BuildConfig

object ApiClient {
    // Base URL is provided per build type via BuildConfig.API_BASE_URL
    private const val BASE_URL = BuildConfig.API_BASE_URL

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
