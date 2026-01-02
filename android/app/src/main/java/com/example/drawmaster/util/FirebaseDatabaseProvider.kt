package com.example.drawmaster.util

import com.google.firebase.database.FirebaseDatabase

fun getFirebaseDatabase(): FirebaseDatabase {
    return try {
        val url = com.example.drawmaster.BuildConfig.FIREBASE_DB_URL
        if (url.isNullOrBlank()) FirebaseDatabase.getInstance() else FirebaseDatabase.getInstance(url)
    } catch (e: Exception) {
        FirebaseDatabase.getInstance()
    }
}
