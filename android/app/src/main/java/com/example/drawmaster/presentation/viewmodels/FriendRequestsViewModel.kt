package com.example.drawmaster.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drawmaster.data.network.FirebaseTokenProvider
import com.example.drawmaster.data.repository.FriendRepositoryImpl
import com.example.drawmaster.domain.model.FriendRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FriendRequestsViewModel : ViewModel() {

    private val tokenProvider = FirebaseTokenProvider()
    private val repository = FriendRepositoryImpl(tokenProvider = tokenProvider)

    private val _requests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val requests: StateFlow<List<FriendRequest>> = _requests

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _sendError = MutableStateFlow<String?>(null)
    val sendError: StateFlow<String?> = _sendError
    private val _sendSuccess = MutableStateFlow<String?>(null)
    val sendSuccess: StateFlow<String?> = _sendSuccess

    private val _outgoing = MutableStateFlow<List<FriendRequest>>(emptyList())
    val outgoing: StateFlow<List<FriendRequest>> = _outgoing

    private val _friends = MutableStateFlow<List<com.example.drawmaster.domain.model.Friend>>(emptyList())
    val friends: StateFlow<List<com.example.drawmaster.domain.model.Friend>> = _friends

    init {
        load()
        loadOutgoing()
        loadFriends()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val res = repository.getPendingRequests()
                _requests.value = res
            } finally {
                _loading.value = false
            }
        }
    }

    fun accept(requestId: Int, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _loading.value = true
            val err = repository.acceptRequest(requestId)
            if (err != null) {
                _error.value = err
            }
            // refresh list regardless
            _requests.value = repository.getPendingRequests()
            _loading.value = false
            onComplete(err == null)
        }
    }

    fun reject(requestId: Int, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _loading.value = true
            val err = repository.rejectRequest(requestId)
            if (err != null) {
                _error.value = err
            }
            _requests.value = repository.getPendingRequests()
            // also refresh outgoing and friends
            _outgoing.value = repository.getOutgoingRequests()
            _friends.value = repository.getFriends()
            _loading.value = false
            onComplete(err == null)
        }
    }

    fun sendByEmail(email: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _loading.value = true
            val err = repository.sendRequestByEmail(email)
            if (err != null) {
                // try to parse JSON error body like {"error":"msg"}
                val parsed = try {
                    val obj = org.json.JSONObject(err)
                    obj.optString("error", err)
                } catch (_: Exception) { err }
                _sendError.value = parsed
                _sendSuccess.value = null
            } else {
                _sendSuccess.value = "Request sent"
                _sendError.value = null
                // refresh outgoing list so it appears under the input
                _outgoing.value = repository.getOutgoingRequests()
            }
            // refresh friends list as well
            _friends.value = repository.getFriends()
            _loading.value = false
            onComplete(err == null)
        }
    }

    private fun loadOutgoing() {
        viewModelScope.launch {
            try {
                _outgoing.value = repository.getOutgoingRequests()
            } catch (e: Exception) { /* ignore */ }
        }
    }

    private fun loadFriends() {
        viewModelScope.launch {
            try {
                _friends.value = repository.getFriends()
            } catch (e: Exception) { /* ignore */ }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSendMessages() {
        _sendError.value = null
        _sendSuccess.value = null
    }
}
