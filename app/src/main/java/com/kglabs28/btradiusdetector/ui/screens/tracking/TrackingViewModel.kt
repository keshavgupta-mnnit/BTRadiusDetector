package com.kglabs28.btradiusdetector.ui.screens.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kglabs28.btradiusdetector.BTRadiusDetectorApp
import com.kglabs28.btradiusdetector.data.AlertSettingsRepository
import com.kglabs28.btradiusdetector.data.local.AppDatabase
import com.kglabs28.btradiusdetector.domain.model.SignalPoint
import com.kglabs28.btradiusdetector.domain.usecase.TrackSignalUseCase
import com.kglabs28.btradiusdetector.utils.AppUtils
import com.kglabs28.btradiusdetector.utils.BluetoothUtils
import com.kglabs28.btradiusdetector.utils.CompassUtils
import com.kglabs28.btradiusdetector.utils.Constants
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
 * Thin wiring only: combines system streams into [uiState] and feeds
 * readings into [TrackSignalUseCase]. Hardware reads go straight to the
 * Bluetooth/compass utils.
 */
class TrackingViewModel(
    private val address: String,
    private val trackSignal: TrackSignalUseCase
) : ViewModel() {

    private val appContext get() = BTRadiusDetectorApp.appContext

    private val device = BluetoothUtils.getBondedDevicesFlow(appContext)
        .map { devices -> devices.find { it.address == address } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val rssi = BluetoothUtils.getRssiFlow(appContext, address)
        .onStart { emit(Constants.RSSI_FLOOR) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Constants.RSSI_FLOOR)

    private val battery = BluetoothUtils.getBatteryFlow(appContext, address)
        .onStart { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val heading = CompassUtils.getHeadingFlow(appContext)
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
        fun factory(deviceId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = BTRadiusDetectorApp.appContext
                    val settings = AlertSettingsRepository(
                        AppDatabase.getInstance(app).alertSettingsDao()
                    )
                    return TrackingViewModel(
                        address = deviceId,
                        trackSignal = TrackSignalUseCase(settings)
                    ) as T
                }
            }
    }
}
