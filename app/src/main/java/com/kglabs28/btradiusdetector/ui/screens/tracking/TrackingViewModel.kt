package com.kglabs28.btradiusdetector.ui.screens.tracking

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kglabs28.btradiusdetector.data.AlertSettingsRepository
import com.kglabs28.btradiusdetector.data.BleRssiRepository
import com.kglabs28.btradiusdetector.data.CompassRepository
import com.kglabs28.btradiusdetector.data.local.AppDatabase
import com.kglabs28.btradiusdetector.domain.model.SignalPoint
import com.kglabs28.btradiusdetector.domain.usecase.ObserveBatteryUseCase
import com.kglabs28.btradiusdetector.domain.usecase.ObserveBondedDevicesUseCase
import com.kglabs28.btradiusdetector.domain.usecase.ObserveDeviceUseCase
import com.kglabs28.btradiusdetector.domain.usecase.ObserveHeadingUseCase
import com.kglabs28.btradiusdetector.domain.usecase.ObserveRssiUseCase
import com.kglabs28.btradiusdetector.domain.usecase.TrackSignalUseCase
import com.kglabs28.btradiusdetector.utils.AppUtils
import com.kglabs28.btradiusdetector.utils.Constants
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the tracking screen renders, observed as one flow. */
data class TrackingUiState(
    val deviceName: String = "",
    val isConnected: Boolean = true,
    val heading: Float = 0f,
    val cardinal: String = "N",
    val rssi: Int = Constants.RSSI_FLOOR,
    val battery: Int? = null,
    val peakRssi: Int = Constants.RSSI_FLOOR,
    val peakHeading: Float = 0f,
    val history: List<SignalPoint> = emptyList()
)

/**
 * Thin wiring only: combines use-case streams into [uiState] and feeds
 * readings into [TrackSignalUseCase]. All logic lives in the use cases.
 */
class TrackingViewModel(
    private val address: String,
    observeBondedDevices: ObserveBondedDevicesUseCase,
    observeRssi: ObserveRssiUseCase,
    observeBattery: ObserveBatteryUseCase,
    observeHeading: ObserveHeadingUseCase,
    observeDevice: ObserveDeviceUseCase,
    private val trackSignal: TrackSignalUseCase
) : ViewModel() {

    private val device = observeDevice(address)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val rssi = observeRssi(address)
        .onStart { emit(Constants.RSSI_FLOOR) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Constants.RSSI_FLOOR)

    private val battery = observeBattery(address)
        .onStart { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val heading = observeHeading()
        .onStart { emit(0f) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val uiState: StateFlow<TrackingUiState> = combine(
        device, heading, rssi, battery, trackSignal.observeSnapshot()
    ) { device, heading, rssi, battery, snapshot ->
        TrackingUiState(
            deviceName = device?.name ?: "",
            isConnected = device?.isConnected ?: true,
            heading = heading,
            cardinal = AppUtils.getCardinalDirection(heading),
            rssi = rssi,
            battery = battery,
            peakRssi = snapshot.peakRssi,
            peakHeading = snapshot.peakHeading,
            history = snapshot.history
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrackingUiState())

    init {
        observeBondedDevices.refresh()
        viewModelScope.launch { trackSignal.restore(address) }
        viewModelScope.launch {
            combine(rssi, heading) { rssi, heading -> rssi to heading }
                .collect { (rssi, heading) -> trackSignal.ingest(address, rssi, heading) }
        }
    }

    fun onExit() {
        trackSignal.reset()
    }

    companion object {
        fun factory(context: Context, deviceId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val ble = BleRssiRepository(appContext)
                    val settings = AlertSettingsRepository(
                        AppDatabase.getInstance(appContext).alertSettingsDao()
                    )
                    val observeBondedDevices = ObserveBondedDevicesUseCase(ble)
                    return TrackingViewModel(
                        address = deviceId,
                        observeBondedDevices = observeBondedDevices,
                        observeRssi = ObserveRssiUseCase(ble),
                        observeBattery = ObserveBatteryUseCase(ble),
                        observeHeading = ObserveHeadingUseCase(CompassRepository(appContext)),
                        observeDevice = ObserveDeviceUseCase(observeBondedDevices),
                        trackSignal = TrackSignalUseCase(settings)
                    ) as T
                }
            }
    }
}
