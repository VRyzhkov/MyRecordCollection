package com.example.myrecordcollection.input

import android.content.Context
import android.media.session.MediaController
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SteeringWheelControllerTest {
    @Test
    fun keysAreConsumedWithoutTouchAndAfterResume() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var boundController: MediaController? = null
        val controller = SteeringWheelController(context) { boundController = it }
        val originalMappings = controller.mappings.value
        try {
            controller.resetDefaults()
            controller.setActivityResumed(true)
            controller.setEnabled(true)
            assertNotNull(boundController)
            for (key in listOf(KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN)) {
                assertTrue(controller.handleKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key)))
                assertTrue(controller.handleKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key)))
            }
            controller.setActivityResumed(false)
            assertNull(boundController)
            assertFalse(controller.handleKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP)))
            controller.setActivityResumed(true)
            assertNotNull(boundController)
            assertTrue(controller.handleKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP)))
            assertTrue(controller.handleKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP)))
            assertFalse(controller.handleKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A)))
        } finally {
            controller.clearAllMappings()
            originalMappings.forEach { (command, key) ->
                controller.startLearning(command)
                controller.handleKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
                controller.handleKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
            }
            controller.release()
        }
    }
}
