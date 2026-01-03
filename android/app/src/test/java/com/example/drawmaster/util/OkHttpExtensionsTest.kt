package com.example.drawmaster.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

/**
 * Simple fake Call implementation that forwards the callback synchronously.
 */
class FakeCall(private val onEnqueue: (Callback) -> Unit, private val requestObj: Request) : Call {
    private var executed = false
    override fun enqueue(responseCallback: Callback) {
        onEnqueue(responseCallback)
    }

    override fun isExecuted(): Boolean = executed
    override fun cancel() {}
    override fun isCanceled(): Boolean = false
    override fun clone(): Call = FakeCall(onEnqueue, requestObj)
    override fun execute(): Response = throw UnsupportedOperationException()
    override fun request(): Request = requestObj
    override fun timeout(): okio.Timeout = okio.Timeout.NONE
}

@OptIn(ExperimentalCoroutinesApi::class)
class OkHttpExtensionsTest {

    @Test
    fun awaitResponse_callExtension_success_returnsResponse() = runTest {
        val request = Request.Builder().url("http://localhost/").build()

        val responseBody = ResponseBody.create("text/plain".toMediaType(), "ok")
        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(responseBody)
            .build()

        val call2 = object : Call {
            override fun enqueue(responseCallback: Callback) { responseCallback.onResponse(this, response) }
            override fun isExecuted(): Boolean = false
            override fun cancel() {}
            override fun isCanceled(): Boolean = false
            override fun clone(): Call = this
            override fun execute(): Response = throw UnsupportedOperationException()
            override fun request(): Request = request
            override fun timeout(): okio.Timeout = okio.Timeout.NONE
        }

        val resp = call2.awaitResponse()
        try {
            assertEquals(200, resp.code)
        } finally {
            resp.close()
        }
    }

    @Test
    fun awaitResponse_callExtension_failure_throws() = runTest {
        val request = Request.Builder().url("http://localhost/").build()

        val call = object : Call {
            override fun enqueue(responseCallback: Callback) { responseCallback.onFailure(this, IOException("fail")) }
            override fun isExecuted(): Boolean = false
            override fun cancel() {}
            override fun isCanceled(): Boolean = false
            override fun clone(): Call = this
            override fun execute(): Response = throw UnsupportedOperationException()
            override fun request(): Request = request
            override fun timeout(): okio.Timeout = okio.Timeout.NONE
        }

        try {
            call.awaitResponse()
            fail("Expected IOException")
        } catch (e: IOException) {
            // expected
        }
    }
}
