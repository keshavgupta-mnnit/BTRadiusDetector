package com.kglabs28.btradiusdetector.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val SHOW_ONBOARDING = booleanPreferencesKey("show_onboarding")
        val ALERT_RADIUS = floatPreferencesKey("alert_radius")
        val MONITORED_DEVICES = stringSetPreferencesKey("monitored_devices")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val showOnboarding = preferences[PreferencesKeys.SHOW_ONBOARDING] ?: true
            val alertRadius = preferences[PreferencesKeys.ALERT_RADIUS] ?: 5.0f
            val monitoredDevices = preferences[PreferencesKeys.MONITORED_DEVICES] ?: emptySet()
            UserPreferences(showOnboarding, alertRadius, monitoredDevices)
        }

    suspend fun updateShowOnboarding(showOnboarding: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_ONBOARDING] = showOnboarding
        }
    }

    suspend fun updateAlertRadius(radius: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ALERT_RADIUS] = radius
        }
    }

    suspend fun toggleDeviceMonitoring(address: String, enable: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.MONITORED_DEVICES] ?: emptySet()
            val newSet = if (enable) {
                current + address
            } else {
                current - address
            }
            preferences[PreferencesKeys.MONITORED_DEVICES] = newSet
        }
    }
}

data class UserPreferences(
    val showOnboarding: Boolean,
    val alertRadius: Float,
    val monitoredDevices: Set<String> = emptySet()
)
