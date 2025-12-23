package com.example.drawmaster.data.repository

import com.example.drawmaster.data.network.ApiClient
import com.example.drawmaster.data.network.FriendApi
import com.example.drawmaster.data.network.TokenProvider
import com.example.drawmaster.domain.model.FriendRequest
import com.example.drawmaster.domain.repository.FriendRepository

class FriendRepositoryImpl(private val tokenProvider: TokenProvider) : FriendRepository {
    private val api = ApiClient.createRetrofit(tokenProvider).create(FriendApi::class.java)

    override suspend fun getPendingRequests(): List<FriendRequest> {
        return try {
            // Retrofit doesn't add auth header automatically here; use OkHttp interceptor for production.
            val resp = api.getFriendRequests()
            resp.requests.map { r ->
                // prefer display name, then email; do not expose UID in UI
                val display = r.display_name ?: r.from_email
                FriendRequest(r.id, /* fromUid */ r.from_uid, /* toUid */ "", "pending", r.created_at, display)
            }
        } catch (e: Exception) {
            android.util.Log.e("FriendRepo", "getPendingRequests failed", e)
            emptyList()
        }
    }

    override suspend fun acceptRequest(requestId: Int): String? {
        return try {
            api.acceptFriendRequest(com.example.drawmaster.data.network.AcceptRequestBody(requestId))
            null
        } catch (e: retrofit2.HttpException) {
            val errBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            android.util.Log.e("FriendRepo", "acceptRequest HTTP ${e.code()} body=$errBody", e)
            errBody ?: "HTTP ${e.code()}"
        } catch (e: Exception) {
            android.util.Log.e("FriendRepo", "acceptRequest failed", e)
            e.message ?: "unknown error"
        }
    }

    override suspend fun rejectRequest(requestId: Int): String? {
        return try {
            api.rejectFriendRequest(com.example.drawmaster.data.network.RejectRequestBody(requestId))
            null
        } catch (e: retrofit2.HttpException) {
            val errBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            android.util.Log.e("FriendRepo", "rejectRequest HTTP ${e.code()} body=$errBody", e)
            errBody ?: "HTTP ${e.code()}"
        } catch (e: Exception) {
            android.util.Log.e("FriendRepo", "rejectRequest failed", e)
            e.message ?: "unknown error"
        }
    }

    override suspend fun sendRequestByEmail(email: String): String? {
        return try {
            api.sendFriendRequest(com.example.drawmaster.data.network.SendRequestBody(email))
            null
        } catch (e: retrofit2.HttpException) {
            val errBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            android.util.Log.e("FriendRepo", "sendRequestByEmail HTTP ${e.code()} body=$errBody", e)
            errBody ?: "HTTP ${e.code()}"
        } catch (e: Exception) {
            android.util.Log.e("FriendRepo", "sendRequestByEmail failed", e)
            e.message ?: "unknown error"
        }
    }

    override suspend fun getOutgoingRequests(): List<FriendRequest> {
        return try {
            val resp = api.getSentFriendRequests()
            resp.requests.map { r ->
                // prefer display name, then email; do not expose UID in UI
                val display = r.display_name ?: r.to_email
                FriendRequest(r.id, /* fromUid */ "", /* toUid */ r.to_uid ?: "", "pending", r.created_at, display)
            }
        } catch (e: Exception) {
            android.util.Log.e("FriendRepo", "getOutgoingRequests failed", e)
            emptyList()
        }
    }

    override suspend fun getFriends(): List<com.example.drawmaster.domain.model.Friend> {
        return try {
            val resp = api.getFriends()
            resp.friends.map { f ->
                val display = f.display_name ?: f.email
                com.example.drawmaster.domain.model.Friend(f.friend_uid, display, f.email)
            }
        } catch (e: Exception) {
            android.util.Log.e("FriendRepo", "getFriends failed", e)
            emptyList()
        }
    }
}
