package com.example.drawmaster.domain.repository

import com.example.drawmaster.domain.model.FriendRequest

interface FriendRepository {
    suspend fun getPendingRequests(): List<FriendRequest>
    // action methods return an error message string on failure, or null on success
    suspend fun acceptRequest(requestId: Int): String?
    suspend fun rejectRequest(requestId: Int): String?
    suspend fun sendRequestByEmail(email: String): String?

    // outgoing pending requests the user has sent
    suspend fun getOutgoingRequests(): List<FriendRequest>

    // user's friends
    suspend fun getFriends(): List<com.example.drawmaster.domain.model.Friend>
}
