package com.example.drawmaster.presentation.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

class ShakeDetector(
    context: Context,
    private val onShake: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    companion object {
        // Threshold for detecting a shake (1.5G is a moderate movement)
        private const val SHAKE_THRESHOLD_GRAVITY = 1.5f

        // Ignoring multiple shakes if they happen too quickly (< 500ms apart)
        private const val SHAKE_SLOP_TIME = 500
    }

    private var shakeTimestamp: Long = 0
    private var isEnabled: Boolean = false

    fun start() {
        if (accelerometer != null && !isEnabled) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            isEnabled = true
            Log.d("SensorTest", "Accelerometer enabled")
        } else if (accelerometer == null) {
            Log.e("SensorTest", "ERROR: Accelerometer not enabled!")
        }
    }

    fun stop() {
        if (isEnabled) {
            sensorManager.unregisterListener(this)
            isEnabled = false
        }
    }

    fun pause() {
        stop()
    }

    fun resume() {
        start()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isEnabled) return

        // Normalising acceleration values by Earth's gravity
        val gX = event.values[0] / SensorManager.GRAVITY_EARTH
        val gY = event.values[1] / SensorManager.GRAVITY_EARTH
        val gZ = event.values[2] / SensorManager.GRAVITY_EARTH

        // Calculating total gForce
        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

        // Checking if force exceeds threshold (to detect a potential shake)
        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            // Ignoring shakes that are too close together
            val now = System.currentTimeMillis()
            if (shakeTimestamp + SHAKE_SLOP_TIME > now) {
                return
            }
            shakeTimestamp = now
            // Notifying shake detected
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}