package com.example.drawmaster.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class FriendDto(val friend_uid: String, val email: String?, val display_name: String?)
data class FriendsResponse(val friends: List<FriendDto>)

data class FriendRequestDto(val id: Int, val from_uid: String, val to_uid: String?, val from_email: String?, val to_email: String?, val display_name: String?, val created_at: String?)
data class FriendRequestsResponse(val requests: List<FriendRequestDto>)

data class AcceptRequestBody(val request_id: Int)
data class RejectRequestBody(val request_id: Int)
data class SendRequestBody(val to_email: String?)

interface FriendApi {
    @GET("/friends")
    suspend fun getFriends(): FriendsResponse

    @GET("/friends/requests")
    suspend fun getFriendRequests(): FriendRequestsResponse

    @GET("/friends/requests/sent")
    suspend fun getSentFriendRequests(): FriendRequestsResponse

    @POST("/friends/accept")
    suspend fun acceptFriendRequest(@Body body: AcceptRequestBody): Any

    @POST("/friends/reject")
    suspend fun rejectFriendRequest(@Body body: RejectRequestBody): Any

    @POST("/friends/request")
    suspend fun sendFriendRequest(@Body body: SendRequestBody): FriendRequestDto
}
