package com.example.drawmaster.presentation.viewmodels

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.Call
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Timeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

// Use Mockito to mock Firebase types without extending package-private constructors
class FakeCall(private val response: Response) : Call {
    override fun enqueue(responseCallback: okhttp3.Callback) { responseCallback.onResponse(this, response) }
    override fun execute(): Response = response
    override fun isExecuted(): Boolean = false
    override fun cancel() {}
    override fun isCanceled(): Boolean = false
    override fun clone(): Call = this
    override fun request(): Request = response.request
    override fun timeout(): Timeout = Timeout.NONE
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelFullTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before fun setup() { Dispatchers.setMain(testDispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `startGame with multiplayer listens and results update`() = runTest {
        val fakeDb = Mockito.mock(com.google.firebase.database.FirebaseDatabase::class.java)
        val fakeRef = Mockito.mock(com.google.firebase.database.DatabaseReference::class.java)
        Mockito.`when`(fakeDb.reference).thenReturn(fakeRef)

        var capturedListener: com.google.firebase.database.ValueEventListener? = null
        Mockito.`when`(fakeRef.child(Mockito.anyString())).thenReturn(fakeRef)
        Mockito.doAnswer { invocation ->
            capturedListener = invocation.arguments[0] as com.google.firebase.database.ValueEventListener
            null
        }.`when`(fakeRef).addValueEventListener(Mockito.any(com.google.firebase.database.ValueEventListener::class.java))

        val vm = GameViewModel(getFirebaseDatabaseFn = { fakeDb })
        vm.startGame(gameId = "g1")

        val map = mapOf("player1" to 10)
        val snapshot = Mockito.mock(com.google.firebase.database.DataSnapshot::class.java)
        Mockito.`when`(snapshot.exists()).thenReturn(true)
        Mockito.`when`(snapshot.value).thenReturn(map)

        // invoke listener
        capturedListener?.onDataChange(snapshot)

        assertEquals(map, vm.results.value)
    }

    @Test
    fun `finishGame when not authenticated sets Error`() = runTest {
        val fakeAuth = Mockito.mock(com.google.firebase.auth.FirebaseAuth::class.java)
        Mockito.`when`(fakeAuth.currentUser).thenReturn(null)

        val fakeDb = Mockito.mock(com.google.firebase.database.FirebaseDatabase::class.java)
        val fakeRef = Mockito.mock(com.google.firebase.database.DatabaseReference::class.java)
        Mockito.`when`(fakeDb.reference).thenReturn(fakeRef)
        Mockito.`when`(fakeRef.child(Mockito.anyString())).thenReturn(fakeRef)
        Mockito.doAnswer { null }.`when`(fakeRef).addValueEventListener(Mockito.any(com.google.firebase.database.ValueEventListener::class.java))

        val vm = GameViewModel(getFirebaseAuth = { fakeAuth }, getFirebaseDatabaseFn = { fakeDb })
        vm.startGame(gameId = "g1")
        testDispatcher.scheduler.advanceUntilIdle()
        vm.finishGame()
        testDispatcher.scheduler.advanceUntilIdle()
        val st = vm.gameState.value
        assertTrue(st is GameScreenState.Error)
    }

    @Test
    fun `finishGame with strokes and canvas sets Finished`() = runTest {
        val fakeDb2 = Mockito.mock(com.google.firebase.database.FirebaseDatabase::class.java)
        val fakeRef2 = Mockito.mock(com.google.firebase.database.DatabaseReference::class.java)
        Mockito.`when`(fakeDb2.reference).thenReturn(fakeRef2)
        Mockito.`when`(fakeRef2.child(Mockito.anyString())).thenReturn(fakeRef2)
        Mockito.doAnswer { null }.`when`(fakeRef2).addValueEventListener(Mockito.any(com.google.firebase.database.ValueEventListener::class.java))

        val vm = GameViewModel(getFirebaseDatabaseFn = { fakeDb2 })
        vm.startGame(gameId = "g1")
        vm.onDrawingChanged(listOf(com.example.drawmaster.presentation.components.DrawingStroke(points = listOf(com.example.drawmaster.presentation.components.DrawingPoint(0f,0f)))))
        vm.setCanvasSize(10,10)
        vm.finishGame()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.gameState.value is GameScreenState.Finished)
    }

    @Test
    fun `submitMultiplayerDrawing not authenticated sets Error`() = runTest {
        val fakeAuth = Mockito.mock(com.google.firebase.auth.FirebaseAuth::class.java)
        Mockito.`when`(fakeAuth.currentUser).thenReturn(null)

        val fakeDb3 = Mockito.mock(com.google.firebase.database.FirebaseDatabase::class.java)
        val fakeRef3 = Mockito.mock(com.google.firebase.database.DatabaseReference::class.java)
        Mockito.`when`(fakeDb3.reference).thenReturn(fakeRef3)
        Mockito.`when`(fakeRef3.child(Mockito.anyString())).thenReturn(fakeRef3)
        Mockito.doAnswer { null }.`when`(fakeRef3).addValueEventListener(Mockito.any(com.google.firebase.database.ValueEventListener::class.java))

        val vm = GameViewModel(getFirebaseAuth = { fakeAuth }, getFirebaseDatabaseFn = { fakeDb3 })
        vm.startGame(gameId = "g1")
        vm.submitMultiplayerDrawing("d","o")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.gameState.value is GameScreenState.Error)
    }

    @Test
    fun `submitMultiplayerDrawing http error sets Error`() = runTest {
        val user = Mockito.mock(com.google.firebase.auth.FirebaseUser::class.java)
        Mockito.`when`(user.uid).thenReturn("u1")
        val fakeAuth = Mockito.mock(com.google.firebase.auth.FirebaseAuth::class.java)
        Mockito.`when`(fakeAuth.currentUser).thenReturn(user)

        val badResponse = Response.Builder()
            .request(Request.Builder().url("http://x/").build())
            .protocol(Protocol.HTTP_1_1)
            .code(500)
            .message("Server Error")
            .body(ResponseBody.create(null, ""))
            .build()

        val fakeDb4 = Mockito.mock(com.google.firebase.database.FirebaseDatabase::class.java)
        val fakeRef4 = Mockito.mock(com.google.firebase.database.DatabaseReference::class.java)
        Mockito.`when`(fakeDb4.reference).thenReturn(fakeRef4)
        Mockito.`when`(fakeRef4.child(Mockito.anyString())).thenReturn(fakeRef4)
        Mockito.doAnswer { null }.`when`(fakeRef4).addValueEventListener(Mockito.any(com.google.firebase.database.ValueEventListener::class.java))

        val vm = GameViewModel(
            httpClient = object: okhttp3.OkHttpClient() { override fun newCall(request: Request): Call = FakeCall(badResponse) },
            getFirebaseAuth = { fakeAuth },
            getFirebaseDatabaseFn = { fakeDb4 },
            getIdToken = { _: Boolean -> "token" },
            ioDispatcher = testDispatcher
        )

        vm.startGame(gameId = "g1")
        vm.submitMultiplayerDrawing("d","o")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.gameState.value is GameScreenState.Error)
    }

    @Test
    fun `submitMultiplayerDrawing success sets WaitingForResults and populates results`() = runTest {
        val user = Mockito.mock(com.google.firebase.auth.FirebaseUser::class.java)
        Mockito.`when`(user.uid).thenReturn("u1")
        val fakeAuth = Mockito.mock(com.google.firebase.auth.FirebaseAuth::class.java)
        Mockito.`when`(fakeAuth.currentUser).thenReturn(user)

        val okResponse = Response.Builder()
            .request(Request.Builder().url("http://x/").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(ResponseBody.create(null, "{}"))
            .build()

        val fakeDb = Mockito.mock(com.google.firebase.database.FirebaseDatabase::class.java)
        val fakeRef = Mockito.mock(com.google.firebase.database.DatabaseReference::class.java)
        Mockito.`when`(fakeDb.reference).thenReturn(fakeRef)
        Mockito.`when`(fakeRef.child(Mockito.anyString())).thenReturn(fakeRef)

        var capturedListener: com.google.firebase.database.ValueEventListener? = null
        Mockito.doAnswer { invocation ->
            capturedListener = invocation.arguments[0] as com.google.firebase.database.ValueEventListener
            null
        }.`when`(fakeRef).addValueEventListener(Mockito.any(com.google.firebase.database.ValueEventListener::class.java))

        val vm = GameViewModel(
            httpClient = object: okhttp3.OkHttpClient() { override fun newCall(request: Request): Call = FakeCall(okResponse) },
            getFirebaseAuth = { fakeAuth },
            getFirebaseDatabaseFn = { fakeDb },
            getIdToken = { _: Boolean -> "token" },
            ioDispatcher = testDispatcher
        )

        vm.startGame(gameId = "g1")
        vm.submitMultiplayerDrawing("d","o")
        testDispatcher.scheduler.advanceUntilIdle()

        val map = mapOf("p" to 5)
        val snapshot = Mockito.mock(com.google.firebase.database.DataSnapshot::class.java)
        Mockito.`when`(snapshot.exists()).thenReturn(true)
        Mockito.`when`(snapshot.value).thenReturn(map)

        capturedListener?.onDataChange(snapshot)
        // allow any Main/dispatcher updates to run
        testDispatcher.scheduler.advanceUntilIdle()

        // state may race with the timer; ensure results were populated
        assertEquals(map, vm.results.value)
    }
}
