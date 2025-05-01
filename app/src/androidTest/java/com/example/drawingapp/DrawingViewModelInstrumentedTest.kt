package com.example.drawingapp

import android.view.MotionEvent
import android.graphics.Color
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class DrawingViewModelInstrumentedTest {

    /**
     * A shared view model for testing purposes. Usage of this member isn't guaranteed to
     * be accessing an unmodified state.
     */
    private lateinit var sharedViewModel: DrawingViewModel

    /**
     * Creates a new Main Activity. Sets the class member above to be the
     * view model for that activity. Properties and methods for the vm can
     * be accessed via that member.
     *
     * HOWEVER: this instance is shared across all tests. If one test needs a
     * specific object state to function, the utility method at the bottom should
     * be used instead. Will creating many activities each with their own view models
     * affect test performance? Idk, probably.
     */
    @Before
    fun setup() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity -> sharedViewModel = activity.viewModel }
    }


    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.drawingapp", appContext.packageName)
    }

    /**
     * Tests whether setting the erase mode toggles the erasure state.
     * Uses the utility function withViewModel and braces to execute the contents
     * of the braces on the main ui thread. The with clause just means I don't have to
     * type viewModel over and over (just so you remember). The withViewModel function
     * provides the view model for the test.
     */
    @Test
    fun test_setEraseMode() {
        withViewModel { viewModel ->
            with(viewModel) {
                assertFalse(isErasing.value)
                setEraseMode(true)
                assertTrue(isErasing.value)
                setEraseMode(false)
                assertFalse(isErasing.value)
            }
        }
    }

    /**
     * Draw a horizontal blue line on the bitmap. Check to see (via observation) if a point
     * in the middle of the line has been filled in i.e., colored blue. This ensures the
     * line is drawn fully from one point to the other correctly. Or is that assuming too much?
     * Well, we already know it works.
     */
    @Test fun testDrawLineOnCanvas() {
        withViewModel { vm ->
            with(vm) {
                val color = 0xFF0011CC.toInt()
                changePaintColor(color) // Blue
                createBitmap()

                val xStart = 20f
                val yStart = 25f
                val xFin = 50f
                val yFin = 25f

                val downEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, xStart, yStart, 0)
                val upEvent = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, xFin, yFin, 0)

                handleTouchEvent(downEvent)
                handleTouchEvent(upEvent)

                val bmp = bitmap.value
                assertNotNull(bmp) // Ensure bitmap is not null
                bmp?.let {
                    // Check that a pixel in the path has the expected color
                    assertEquals(color, it.getPixel(30, 25))
                }
            }
        }
    }


    /**
     * If you set the color value, then it should probably set the color value to that color.
     */
    @Test fun test_changePaintColor() {
        val color = 0xFF0000FF.toInt()
        sharedViewModel.changePaintColor(color)
        assertEquals(color, sharedViewModel.currentColor)
    }

    /**
     * Test whether the Paint brush size is set according to the parameter (which does not match
     * the default size).
     */
    @Test fun test_changePaintSize() {
        val currentSize = sharedViewModel.currentStrokeSize.floatValue
        val newSize = currentSize + 1
        sharedViewModel.changePaintSize(newSize)
        assertEquals(newSize, sharedViewModel.currentStrokeSize.floatValue)
        assertNotEquals(currentSize, sharedViewModel.currentStrokeSize.floatValue)
    }

    /**
     * Test whether the dimensions are set according to the parameters.
     */
    @Test fun test_setImageViewDimensions() {
        val w = 1024
        val h = 720
        sharedViewModel.setImageViewDimensions(w, h)
        assertEquals(w, sharedViewModel.imageViewWidth)
        assertEquals(h, sharedViewModel.imageViewHeight)
    }

    /**
     * Through myriad failures, I have determined that you cannot mess with Live Datas
     * except on the main (UI) thread. So, here is a way around that.
     * You can run this function within a test to perform anything within the lambda's
     * braces on the main thread. Testing has never been easier. Yay.
     */
    // utility functions (to set up vars, etc.)
    private fun withViewModel(test: (DrawingViewModel) -> Unit) {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            val viewModel = activity.viewModel
            activity.runOnUiThread {
                test(viewModel)
            }
        }
    }
}
