package com.kglabs28.btradiusdetector.ui.screens.rangealerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kglabs28.btradiusdetector.BTRadiusDetectorApp
import com.kglabs28.btradiusdetector.data.AlertSettingsRepository
import com.kglabs28.btradiusdetector.data.local.AppDatabase
import com.kglabs28.btradiusdetector.domain.model.BluetoothDeviceModel
import com.kglabs28.btradiusdetector.utils.BluetoothUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the range alerts screen renders, observed as one flow. */
data class RangeAlertsUiState(
    val devices: List<BluetoothDeviceModel> = emptyList(),
    val alertEnabled: Map<String, Boolean> = emptyMap()
)

/**
 * Thin wiring only: combines the bonded-device stream with stored flags into
 * [uiState]. Bluetooth reads go straight to [BluetoothUtils].
 */
class RangeAlertsViewModel(
    private val settingsRepository: AlertSettingsRepository
) : ViewModel() {

    private val appContext get() = BTRadiusDetectorApp.appContext

    private val refreshTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val bondedDevices = refreshTrigger
        .flatMapLatest { BluetoothUtils.getBondedDevicesFlow(appContext) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<BluetoothDeviceModel>())

    private val storedSettings = settingsRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Single state for the screen: devices + effective master flags. */
    val uiState: StateFlow<RangeAlertsUiState> =
        combine(bondedDevices, storedSettings) { devices, rows ->
            RangeAlertsUiState(
                devices = devices,
                // Strict opt-in: no stored row means the device was never
                // registered — toggling it on creates an all-true row.
                alertEnabled = devices.associate { device ->
                    device.address to (rows.find { it.address == device.address }?.monitoringEnabled ?: false)
                }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RangeAlertsUiState())

    fun refresh() {
        refreshTrigger.value += 1
    }

    fun setAlertEnabled(address: String, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMonitoringEnabled(address, enabled)
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = BTRadiusDetectorApp.appContext
                return RangeAlertsViewModel(
                    AlertSettingsRepository(AppDatabase.getInstance(app).alertSettingsDao())
                ) as T
            }
        }
    }
}
