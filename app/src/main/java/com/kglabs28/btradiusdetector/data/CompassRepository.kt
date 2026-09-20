package com.kglabs28.btradiusdetector.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Repository for handling sensor data and providing device heading (azimuth).
 */
class CompassRepository(private val context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    /**
     * Returns a Flow of device heading in degrees (0 to 360).
     */
    fun getHeadingFlow(): Flow<Float> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    
                    // orientation[0] is azimuth, rotation around the -Z axis.
                    // It represents the angle between the device's y-axis and the magnetic north pole.
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
