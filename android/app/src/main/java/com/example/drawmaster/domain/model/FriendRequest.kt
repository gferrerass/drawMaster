package com.example.drawmaster.domain.model

data class FriendRequest(
    val id: Int,
    val fromUid: String,
    val toUid: String,
    val status: String,
    val createdAt: String?,
    val displayName: String? = null
)
