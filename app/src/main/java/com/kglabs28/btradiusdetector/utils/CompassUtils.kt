package com.kglabs28.btradiusdetector.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Direct rotation-sensor access: device heading (azimuth) in degrees (0–360).
 * Plain function taking [Context] — no instances, no stored state.
 */
object CompassUtils {

    fun getHeadingFlow(context: Context): Flow<Float> = callbackFlow {
        val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)

                    // orientation[0] is azimuth, rotation around the -Z axis.
                    var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    if (azimuth < 0) {
                        azimuth += 360f
                    }
                    trySend(azimuth)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationVectorSensor == null) {
            close(Exception("Rotation Vector sensor not available on this device."))
            return@callbackFlow
        }

        sensorManager.registerListener(
            listener,
            rotationVectorSensor,
            SensorManager.SENSOR_DELAY_UI
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
