package com.example.drawmaster.data.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenProvider: TokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val token = try {
            tokenProvider.getToken()
        } catch (e: Exception) {
            null
        }

        return if (token.isNullOrEmpty()) {
            chain.proceed(req)
        } else {
            val newReq = req.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(newReq)
        }
    }
}
