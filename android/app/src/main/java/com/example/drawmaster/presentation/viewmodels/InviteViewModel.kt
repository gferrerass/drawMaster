package com.example.drawmaster.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.example.drawmaster.util.getFirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.drawmaster.util.getIdTokenSuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.drawmaster.util.setValueAwait
import com.example.drawmaster.util.updateChildrenAwait
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.RequestBody
import okhttp3.Headers
import org.json.JSONObject
import java.util.UUID
import android.util.Log
import android.os.Handler
import android.os.Looper
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference

class InviteViewModel : ViewModel() {

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val database = getFirebaseDatabase()
    private val auth = FirebaseAuth.getInstance()
    private val httpClient = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())

    // Incoming invite listener
    data class IncomingInvite(
        val inviteId: String,
        val fromUid: String,
        val fromName: String,
        val gameId: String,
        val createdAt: Long,
        val expiresAt: Long
    )

    private val _incoming = MutableStateFlow<IncomingInvite?>(null)
    val incoming = _incoming.asStateFlow()

    private var invitesRef: DatabaseReference? = null
    private var invitesListener: ChildEventListener? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    fun sendInvite(toUid: String, toDisplayName: String?, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _sending.value = true
            _error.value = null
            try {
                val user = auth.currentUser
                val fromUid = user?.uid ?: "unknown"
                        android.util.Log.i("InviteVM", "sendInvite invoked by uid=$fromUid, target=$toUid")
                val fromName = user?.displayName ?: user?.email ?: "Player"
                val gameId = UUID.randomUUID().toString()
                val inviteRef = database.reference.child("invites").child(toUid).push()
                val inviteId = inviteRef.key ?: UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                val payload = mapOf(
                    "inviteId" to inviteId,
                    "fromUid" to fromUid,
                    "fromName" to fromName,
                    "fromDisplayName" to fromName,
                    "gameId" to gameId,
                    "createdAt" to now,
                    "expiresAt" to (now + 60_000) // expires in 60s
                )

                try {
                    withContext(Dispatchers.IO) {
                        inviteRef.setValueAwait(payload)
                        android.util.Log.i("InviteVM", "invite sent to $toUid gameId=$gameId inviteId=$inviteId payload=$payload")
                        // create initial game node so the inviter can listen to it
                        val gameRef = database.reference.child("games").child(gameId)
                        val gamePayload = mapOf(
                            "gameId" to gameId,
                            "playerA" to fromUid,
                            "state" to "pending",
                            "createdAt" to now
                        )
                        try {
                            gameRef.setValueAwait(gamePayload)
                        } catch (e: Exception) {
                            android.util.Log.w("InviteVM", "failed to create initial game node: ", e)
                        }
                    }
                    _sending.value = false
                    onComplete(true, gameId)
                } catch (e: Exception) {
                    _sending.value = false
                    val msg = e.message ?: "failed to send"
                    _error.value = msg
                    android.util.Log.w("InviteVM", "invite failed to send: $msg", e)
                    onComplete(false, msg)
                }
            } catch (e: Exception) {
                _sending.value = false
                _error.value = e.message
                onComplete(false, e.message)
            }
        }
    }

    

    /** Start listening for invites addressed to the current user. */
    fun startListeningForInvites() {
        val uid = auth.currentUser?.uid
        // if not signed in yet, wait for auth state and try again
        if (uid == null) {
            if (authStateListener == null) {
                authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                    val u = firebaseAuth.currentUser
                    if (u != null) {
                        // remove listener and start listening now
                        try { auth.removeAuthStateListener(authStateListener!!) } catch (_: Exception) {}
                        authStateListener = null
                        startListeningForInvites()
                    }
                }
                auth.addAuthStateListener(authStateListener!!)
            }
            return
        }

        // avoid double-registering
        if (invitesRef != null) return
        val ref = database.reference.child("invites").child(uid)
        invitesRef = ref
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    val map = snapshot.value as? Map<*, *> ?: return
                    val inviteId = map["inviteId"] as? String ?: snapshot.key ?: return
                    val fromUid = map["fromUid"] as? String ?: ""
                    val fromName = map["fromName"] as? String ?: map["fromDisplayName"] as? String ?: "Player"
                    val gameId = map["gameId"] as? String ?: ""
                    val createdAt = (map["createdAt"] as? Long) ?: (map["createdAt"] as? Number)?.toLong() ?: 0L
                    val expiresAt = (map["expiresAt"] as? Long) ?: (map["expiresAt"] as? Number)?.toLong() ?: 0L
                    val status = map["status"] as? String ?: "pending"
                    val currentUid = auth.currentUser?.uid
                    android.util.Log.i("InviteVM", "onChildAdded snapshotKey=${snapshot.key} currentUser=$currentUid inviteFrom=$fromUid status=$status data=$map")

                    // ignore invites that are not pending, expired, or sent by self
                    val now = System.currentTimeMillis()
                    if (status != "pending") return
                    if (expiresAt != 0L && expiresAt < now) {
                        // cleanup expired invite
                        try { snapshot.ref.removeValue() } catch (_: Exception) {}
                        return
                    }
                    if (fromUid == currentUid) return

                    _incoming.value = IncomingInvite(inviteId, fromUid, fromName, gameId, createdAt, expiresAt)
                } catch (_: Exception) {}
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                try {
                    val map = snapshot.value as? Map<*, *> ?: return
                    val status = map["status"] as? String ?: "pending"
                    val key = snapshot.key
                    if (status != "pending" && key != null && _incoming.value?.inviteId == key) {
                        _incoming.value = null
                    }
                } catch (_: Exception) {}
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // if the active invite was removed, clear it
                val key = snapshot.key
                if (key != null && _incoming.value?.inviteId == key) {
                    _incoming.value = null
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {
                // permission denied or other DB errors surface here
                android.util.Log.w("InviteVM", "invites listener cancelled: ${error.code} ${error.message}")
                if (error.code == DatabaseError.PERMISSION_DENIED) {
                    _error.value = "Permission denied when listening for invites"
                }
            }
        }
        invitesListener = listener
        ref.addChildEventListener(listener)
    }

    /** Accept the currently received invite (if any). Creates a /games/{gameId} node and removes the invite. */
    fun acceptCurrentInvite(onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        val inv = _incoming.value ?: run { onComplete(false, "no invite"); return }
        viewModelScope.launch {
            try {
                val user = auth.currentUser
                if (user == null) { onComplete(false, "not authenticated"); return@launch }
                try {
                    val idToken = try { user.getIdTokenSuspend(true) } catch (_: Exception) { "" }
                    val apiUrl = com.example.drawmaster.BuildConfig.API_BASE_URL.trimEnd('/') + "/multiplayer/invite/accept"
                    Log.i("InviteVM", "acceptCurrentInvite: apiUrl=$apiUrl inviteId=${inv.inviteId}")
                    val json = "{\"invite_id\":\"${inv.inviteId}\"}"
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = json.toRequestBody(mediaType)
                    val req = Request.Builder()
                        .url(apiUrl)
                        .post(body)
                        .header("Authorization", "Bearer $idToken")
                        .header("Accept", "application/json")
                        .build()
                    val response = withContext(Dispatchers.IO) { httpClient.newCall(req).execute() }
                    response.use { r ->
                        Log.i("InviteVM", "accept response: code=${r.code} message=${r.message}")
                        val bodyStr = try { r.body?.string() } catch (_: Exception) { null }
                        Log.i("InviteVM", "accept response body: $bodyStr")
                        if (!r.isSuccessful) {
                            mainHandler.post { onComplete(false, "http ${r.code}: ${r.message}") }
                            return@launch
                        }
                        // success — remove invite locally
                        try { invitesRef?.child(inv.inviteId)?.removeValue() } catch (e: Exception) { Log.w("InviteVM", "failed removing invite locally", e) }
                        _incoming.value = null
                        // try to parse gameId from response body if present
                        var gameId: String? = null
                        try {
                            if (!bodyStr.isNullOrBlank()) {
                                val jo = JSONObject(bodyStr)
                                if (jo.has("gameId")) gameId = jo.optString("gameId", null)
                            }
                        } catch (e: Exception) { Log.w("InviteVM", "parsing response JSON failed", e) }
                        mainHandler.post { onComplete(true, gameId ?: inv.gameId) }
                    }
                } catch (e: Exception) {
                    Log.w("InviteVM", "acceptCurrentInvite failed", e)
                    mainHandler.post { onComplete(false, e.message) }
                }
            } catch (e: Exception) {
                Log.w("InviteVM", "acceptCurrentInvite exception", e)
                mainHandler.post { onComplete(false, e.message) }
            }
        }
    }

    /** Reject the currently received invite. */
    fun rejectCurrentInvite(onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        val inv = _incoming.value ?: run { onComplete(false, "no invite"); return }
        viewModelScope.launch {
            try {
                // call backend reject endpoint so server records rejection
                val user = auth.currentUser
                if (user == null) { onComplete(false, "not authenticated"); return@launch }

                try {
                    val idToken = try { user.getIdTokenSuspend(true) } catch (_: Exception) { "" }
                    val apiUrl = com.example.drawmaster.BuildConfig.API_BASE_URL.trimEnd('/') + "/multiplayer/invite/reject"
                    Log.i("InviteVM", "rejectCurrentInvite: apiUrl=$apiUrl inviteId=${inv.inviteId}")
                    val json = "{\"invite_id\":\"${inv.inviteId}\"}"
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = json.toRequestBody(mediaType)
                    val req = Request.Builder()
                        .url(apiUrl)
                        .post(body)
                        .header("Authorization", "Bearer $idToken")
                        .header("Accept", "application/json")
                        .build()
                    val response = withContext(Dispatchers.IO) { httpClient.newCall(req).execute() }
                    response.use { r ->
                        Log.i("InviteVM", "reject response: code=${r.code} message=${r.message}")
                        val bodyStr = try { r.body?.string() } catch (_: Exception) { null }
                        Log.i("InviteVM", "reject response body: $bodyStr")
                        if (!r.isSuccessful) {
                            mainHandler.post { onComplete(false, "http ${r.code}: ${r.message}") }
                            return@launch
                        }
                        // remove invite locally
                        try { invitesRef?.child(inv.inviteId)?.removeValue() } catch (e: Exception) { Log.w("InviteVM","failed removing invite locally", e) }
                        _incoming.value = null
                        mainHandler.post { onComplete(true, null) }
                    }
                } catch (e: Exception) {
                    Log.w("InviteVM", "rejectCurrentInvite failed", e)
                    mainHandler.post { onComplete(false, e.message) }
                }
            } catch (e: Exception) {
                Log.w("InviteVM", "rejectCurrentInvite exception", e)
                mainHandler.post { onComplete(false, e.message) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            invitesListener?.let { invitesRef?.removeEventListener(it) }
        } catch (_: Exception) {}
        invitesListener = null
        invitesRef = null
    }
}
