package com.kglabs28.btradiusdetector.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kglabs28.btradiusdetector.data.BleRssiRepository
import com.kglabs28.btradiusdetector.domain.model.BluetoothDeviceModel
import com.kglabs28.btradiusdetector.utils.Strings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * Owns Settings screen state (UDF). UI observes StateFlow, forwards intents.
 */
class SettingsViewModel(bleRepository: BleRssiRepository) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val bondedDevices: StateFlow<List<BluetoothDeviceModel>> = refreshTrigger
        .flatMapLatest { bleRepository.getBondedDevicesFlow() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _overrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val alertOverrides = _overrides.asStateFlow()

    /** Effective per-device alert flag: override ?: device.isConnected. Reactive via combine. */
    val alertEnabled: StateFlow<Map<String, Boolean>> =
        combine(bondedDevices, _overrides) { devices, overrides ->
            devices.associate { it.address to (overrides[it.address] ?: it.isConnected) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _sound = MutableStateFlow(Strings.notificationSoundDefault)
    val sound: StateFlow<String> = _sound.asStateFlow()

    private val _vibrationOn = MutableStateFlow(true)
    val vibrationOn: StateFlow<Boolean> = _vibrationOn.asStateFlow()

    fun refresh() {
        refreshTrigger.value += 1
    }

    fun setAlertEnabled(address: String, enabled: Boolean) {
        _overrides.value = _overrides.value + (address to enabled)
    }

    fun toggleSound() {
        _sound.value = if (_sound.value == Strings.notificationSoundDefault) Strings.notificationSoundAlt
        else Strings.notificationSoundDefault
    }

    fun toggleVibration() {
        _vibrationOn.value = !_vibrationOn.value
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(BleRssiRepository(context.applicationContext)) as T
            }
        }
    }
}
