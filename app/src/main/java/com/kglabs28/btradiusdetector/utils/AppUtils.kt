package com.kglabs28.btradiusdetector.utils

import android.bluetooth.BluetoothClass
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.ui.graphics.vector.ImageVector

object AppUtils {

    // ---- Bluetooth device classification ----

    fun getDeviceIcon(majorClass: Int, minorClass: Int = 0): ImageVector {
        return when (majorClass) {
            BluetoothClass.Device.Major.AUDIO_VIDEO -> when (minorClass) {
                BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER -> Icons.Rounded.Speaker
                BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO -> Icons.Rounded.DirectionsCar
                else -> Icons.Rounded.Headset
            }
            BluetoothClass.Device.Major.WEARABLE -> Icons.Rounded.Watch
            BluetoothClass.Device.Major.COMPUTER -> Icons.Rounded.Computer
            BluetoothClass.Device.Major.PHONE -> Icons.Rounded.PhoneAndroid
            else -> Icons.Rounded.Bluetooth
        }
    }

    fun getDeviceTypeLabel(majorClass: Int, minorClass: Int = 0): String {
        return when (majorClass) {
            BluetoothClass.Device.Major.AUDIO_VIDEO -> when (minorClass) {
                BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER -> "Speaker"
                BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO -> "Car Audio"
                BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET,
                BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES -> "Earbuds"
                else -> "Audio"
            }
            BluetoothClass.Device.Major.WEARABLE -> "Watch"
            BluetoothClass.Device.Major.COMPUTER -> "Computer"
            BluetoothClass.Device.Major.PHONE -> "Phone"
            else -> "Device"
        }
    }

    // ---- Compass ----

    fun getCardinalDirection(degrees: Float): String {
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = (((degrees % 360) / 45.0).let { if (it < 0) it + 8 else it }).toInt() % 8
        return directions[index]
    }

    // ---- Signal strength ----

    /**
     * Normalizes a raw RSSI (dBm, typically -100 to -30) into a 0f..1f
     * progress value for signal-strength bars/dot sizing.
     */
    fun rssiToProgress(rssi: Int): Float {
        val range = (Constants.RSSI_HOT - Constants.RSSI_FLOOR).toFloat()
        val clamped = rssi.coerceIn(Constants.RSSI_FLOOR, Constants.RSSI_HOT + 20)
        return ((clamped - Constants.RSSI_FLOOR) / range).coerceIn(0f, 1f)
    }

    // ---- Time ----

    fun formatElapsedTime(fromMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diffSeconds = ((nowMillis - fromMillis) / 1000).coerceAtLeast(0)
        return when {
            diffSeconds < 60 -> "just now"
            diffSeconds < 3600 -> "${diffSeconds / 60} min ago"
            diffSeconds < 86400 -> "${diffSeconds / 3600} hr ago"
            else -> "${diffSeconds / 86400} days ago"
        }
    }
}