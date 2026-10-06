package com.kglabs28.btradiusdetector.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kglabs28.btradiusdetector.data.AlertSettingsRepository
import com.kglabs28.btradiusdetector.data.BleRssiRepository
import com.kglabs28.btradiusdetector.data.local.AppDatabase
import com.kglabs28.btradiusdetector.domain.model.BluetoothDeviceModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Everything the settings screen renders, observed as one flow.
 */
data class SettingsUiState(
    val devices: List<BluetoothDeviceModel> = emptyList(),
    val alertEnabled: Map<String, Boolean> = emptyMap()
)

/**
 * Owns Settings screen state (UDF). Master toggles persist to Room via
 * [AlertSettingsRepository]; the screen observes [uiState] once.
 */
class SettingsViewModel(
    bleRepository: BleRssiRepository,
    private val settingsRepository: AlertSettingsRepository
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val bondedDevices: StateFlow<List<BluetoothDeviceModel>> = refreshTrigger
        .flatMapLatest { bleRepository.getBondedDevicesFlow() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val storedSettings = settingsRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Single state for the screen: devices + effective master flags. */
    val uiState: StateFlow<SettingsUiState> =
        combine(bondedDevices, storedSettings) { devices, rows ->
            SettingsUiState(
                devices = devices,
                alertEnabled = devices.associate { device ->
                    device.address to (rows.find { it.address == device.address }?.monitoringEnabled
                        ?: device.isConnected)
                }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun refresh() {
        refreshTrigger.value += 1
    }

    fun setAlertEnabled(address: String, enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMonitoringEnabled(address, enabled) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                return SettingsViewModel(
                    BleRssiRepository(appContext),
                    AlertSettingsRepository(AppDatabase.getInstance(appContext).alertSettingsDao())
                ) as T
            }
        }
    }
}
