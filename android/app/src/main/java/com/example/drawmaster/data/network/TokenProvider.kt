package com.example.drawmaster.data.network

interface TokenProvider {
    fun getToken(): String?
}
