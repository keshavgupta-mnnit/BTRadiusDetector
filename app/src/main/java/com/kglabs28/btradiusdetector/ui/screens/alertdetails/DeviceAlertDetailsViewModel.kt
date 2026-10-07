package com.kglabs28.btradiusdetector.ui.screens.alertdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kglabs28.btradiusdetector.BTRadiusDetectorApp
import com.kglabs28.btradiusdetector.data.AlertSettingsRepository
import com.kglabs28.btradiusdetector.data.local.AppDatabase
import com.kglabs28.btradiusdetector.domain.model.AlertSoundMode
import com.kglabs28.btradiusdetector.domain.usecase.ObserveAlertSettingsUseCase
import com.kglabs28.btradiusdetector.domain.usecase.UpdateAlertSettingUseCase
import com.kglabs28.btradiusdetector.utils.BluetoothUtils
import com.kglabs28.btradiusdetector.utils.Strings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the details screen renders, observed as one flow. */
data class AlertDetailsUiState(
    val deviceName: String = Strings.unknownDevice,
    val majorClass: Int = 0,
    val minorClass: Int = 0,
    val isConnected: Boolean = false,
    val monitoringEnabled: Boolean = false,
    val notifyOnDisconnect: Boolean = false,
    val notifyOnReconnect: Boolean = false,
    val soundMode: AlertSoundMode = AlertSoundMode.ONCE,
    val vibrationEnabled: Boolean = true
)

/** Every user intent on this screen funnels through one handler. */
sealed interface AlertDetailsEvent {
    data class MonitoringToggled(val enabled: Boolean) : AlertDetailsEvent
    data class NotifyDisconnectToggled(val enabled: Boolean) : AlertDetailsEvent
    data class NotifyReconnectToggled(val enabled: Boolean) : AlertDetailsEvent
    data class SoundModeSelected(val mode: AlertSoundMode) : AlertDetailsEvent
    data class VibrationToggled(val enabled: Boolean) : AlertDetailsEvent
}

/**
 * Thin wiring only: combines use-case streams into [uiState] and routes
 * [AlertDetailsEvent] to [UpdateAlertSettingUseCase]. All logic lives below.
 */
class DeviceAlertDetailsViewModel(
    private val address: String,
    observeAlerts: ObserveAlertSettingsUseCase,
    private val updateAlerts: UpdateAlertSettingUseCase
) : ViewModel() {

    val uiState: StateFlow<AlertDetailsUiState> = combine(
        BluetoothUtils.getBondedDevicesFlow(BTRadiusDetectorApp.appContext).map { devices ->
            devices.find { it.address == address }
        },
        observeAlerts.observeByAddress(address)
    ) { device, row ->
        AlertDetailsUiState(
            deviceName = device?.name ?: Strings.unknownDevice,
            majorClass = device?.deviceClass ?: 0,
            minorClass = device?.minorDeviceClass ?: 0,
            isConnected = device?.isConnected ?: false,
            monitoringEnabled = row?.monitoringEnabled ?: false,
            notifyOnDisconnect = row?.notifyOnDisconnect ?: false,
            notifyOnReconnect = row?.notifyOnReconnect ?: false,
            soundMode = AlertSoundMode.fromName(row?.soundMode),
            vibrationEnabled = row?.vibrationEnabled ?: false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlertDetailsUiState())

    fun onEvent(event: AlertDetailsEvent) {
        viewModelScope.launch {
            when (event) {
                is AlertDetailsEvent.MonitoringToggled ->
                    updateAlerts.setMonitoring(address, event.enabled)
                is AlertDetailsEvent.NotifyDisconnectToggled ->
                    updateAlerts.setNotifyOnDisconnect(address, event.enabled)
                is AlertDetailsEvent.NotifyReconnectToggled ->
                    updateAlerts.setNotifyOnReconnect(address, event.enabled)
                is AlertDetailsEvent.SoundModeSelected ->
                    updateAlerts.setSoundMode(address, event.mode)
                is AlertDetailsEvent.VibrationToggled ->
                    updateAlerts.setVibration(address, event.enabled)
            }
        }
    }

    companion object {
        fun factory(address: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = BTRadiusDetectorApp.appContext
                    val settings = AlertSettingsRepository(
                        AppDatabase.getInstance(app).alertSettingsDao()
                    )
                    return DeviceAlertDetailsViewModel(
                        address = address,
                        observeAlerts = ObserveAlertSettingsUseCase(settings),
                        updateAlerts = UpdateAlertSettingUseCase(settings)
                    ) as T
                }
            }
    }
}
