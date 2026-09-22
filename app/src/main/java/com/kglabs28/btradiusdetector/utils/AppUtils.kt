package com.kglabs28.btradiusdetector.utils

import android.bluetooth.BluetoothClass
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFS_NAME)

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

    // ---- Onboarding flag (DataStore) ----

    private val SHOW_ONBOARDING_KEY = booleanPreferencesKey(Constants.KEY_SHOW_ONBOARDING)

    fun observeShowOnboarding(context: Context): Flow<Boolean> =
        context.dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { prefs -> prefs[SHOW_ONBOARDING_KEY] ?: true }

    suspend fun setShowOnboarding(context: Context, show: Boolean) {
        context.dataStore.edit { prefs -> prefs[SHOW_ONBOARDING_KEY] = show }
    }
}