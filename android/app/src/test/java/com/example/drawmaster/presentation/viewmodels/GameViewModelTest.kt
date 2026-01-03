package com.example.drawmaster.presentation.viewmodels

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
// JVM unit tests without Robolectric
import com.example.drawmaster.presentation.components.DrawingPoint
import com.example.drawmaster.presentation.components.DrawingStroke

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startGame_setsImageAndClearsStrokes_and_startsTimer() = runTest {
        val vm = GameViewModel()

        vm.startGame(imageName = "test_image", gameId = null)

        assertEquals("test_image", vm.imageName.value)
        assertTrue(vm.strokes.value.isEmpty())
        assertTrue(vm.gameState.value is GameScreenState.Playing)
    }

    @Test
    fun onDrawingChanged_setsHasDrawn_and_clearDrawing_resetsIt() = runTest {
        val vm = GameViewModel()
        vm.startGame()

        val stroke = DrawingStroke(points = listOf(DrawingPoint(1f, 1f)))
        vm.onDrawingChanged(listOf(stroke))

        val st = vm.gameState.value
        assertTrue(st is GameScreenState.Playing && st.hasDrawn)

        vm.clearDrawing()
        val st2 = vm.gameState.value
        assertTrue(st2 is GameScreenState.Playing && !(st2 as GameScreenState.Playing).hasDrawn)
    }

    @Test
    fun undoLastStroke_removesStroke() = runTest {
        val vm = GameViewModel()
        vm.startGame()

        val s1 = DrawingStroke(points = listOf(DrawingPoint(0f, 0f)))
        val s2 = DrawingStroke(points = listOf(DrawingPoint(1f, 1f)))
        vm.onDrawingChanged(listOf(s1, s2))
        assertEquals(2, vm.strokes.value.size)

        vm.undoLastStroke()
        assertEquals(1, vm.strokes.value.size)
    }

    @Test
    fun setters_update_values() = runTest {
        val vm = GameViewModel()

        vm.setStrokeColor(123)
        assertEquals(123, vm.strokeColor.value)

        vm.setStrokeWidth(7f)
        assertEquals(7f, vm.strokeWidth.value)

        vm.setCanvasSize(200, 300)
        assertEquals(200, vm.canvasWidth.value)
        assertEquals(300, vm.canvasHeight.value)
    }

    @Test
    fun finishGame_withoutMultiplayer_setsFinished() = runTest {
        val vm = GameViewModel()
        vm.startGame()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.finishGame(score = 0f)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.gameState.value is GameScreenState.Finished)
    }
}
