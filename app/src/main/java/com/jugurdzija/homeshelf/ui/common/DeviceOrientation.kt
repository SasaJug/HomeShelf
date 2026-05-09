package com.jugurdzija.homeshelf.ui.common

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.atan2
import kotlin.math.sqrt

data class DeviceOrientation(val pitch: Float = 0f, val roll: Float = 0f)

const val LEVEL_THRESHOLD_DEG = 5f

/*
This file provides a custom Jetpack Compose Hook
that  tracks device pitch (via Accelerometer) and roll (via Gravity sensor, if available).
Smoothing is applied for stabilization.
The goal is to help user capture planar reference photo as much as possible.
Note: There is no way to track, so the method is not perfect.
 */

@Composable
fun rememberDeviceOrientation(): State<DeviceOrientation> {
    val context = LocalContext.current
    val orientation = remember { mutableStateOf(DeviceOrientation()) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

        val listener = object : SensorEventListener {
            private var smoothedPitch = 0f
            private var smoothedRoll = 0f
            private val alpha = 0.15f

            override fun onSensorChanged(event: SensorEvent) {
                val vals = event.values

                // Calculate pitch
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val targetPitch = Math.toDegrees(
                        atan2(vals[2].toDouble(), vals[1].toDouble())
                    ).toFloat()
                    smoothedPitch += alpha * (targetPitch - smoothedPitch)
                }

                // Calculate roll
                if (event.sensor.type == Sensor.TYPE_GRAVITY || (gravitySensor == null && event.sensor.type == Sensor.TYPE_ACCELEROMETER)) {
                    val targetRoll = Math.toDegrees(
                        atan2(vals[0].toDouble(), sqrt((vals[1] * vals[1] + vals[2] * vals[2]).toDouble()))
                    ).toFloat()
                    smoothedRoll += alpha * (targetRoll - smoothedRoll)
                }

                orientation.value = DeviceOrientation(smoothedPitch, smoothedRoll)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
        gravitySensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose { sensorManager.unregisterListener(listener) }
    }

    return orientation
}
