package com.kglabs28.btradiusdetector.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kglabs28.btradiusdetector.data.BleRssiRepository
import com.kglabs28.btradiusdetector.data.CompassRepository
import com.kglabs28.btradiusdetector.domain.model.BluetoothDeviceModel
import com.kglabs28.btradiusdetector.utils.AppUtils
import com.kglabs28.btradiusdetector.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    private val bleRepository: BleRssiRepository,
    private val compassRepository: CompassRepository
) : ViewModel() {

    private val _bondedDevices = MutableStateFlow<List<BluetoothDeviceModel>>(emptyList())
    val bondedDevices: StateFlow<List<BluetoothDeviceModel>> = _bondedDevices.asStateFlow()

    private val _selectedDeviceId = MutableStateFlow<String?>(null)
    val selectedDeviceId = _selectedDeviceId.asStateFlow()

    /**
     * Reactive device lookup — filtering lives in the ViewModel (UDF),
     * not in @Composable via remember { devices.find {} }.
     */
    val trackingDevice: StateFlow<BluetoothDeviceModel?> =
        combine(_bondedDevices, _selectedDeviceId) { devices, id ->
            devices.find { it.address == id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _peakRssi = MutableStateFlow(Constants.RSSI_FLOOR)
    val peakRssi = _peakRssi.asStateFlow()

    private val _peakHeading = MutableStateFlow(0f)
    val peakHeading = _peakHeading.asStateFlow()

    private val _signalHistory = MutableStateFlow<List<SignalPoint>>(emptyList())
    val signalHistory = _signalHistory.asStateFlow()

    val headingFlow: Flow<Float> = compassRepository.getHeadingFlow()

    /** Heading cardinal derived reactively via map — no manual mapping in UI. */
    val headingCardinal: Flow<String> = headingFlow.map { AppUtils.getCardinalDirection(it) }

    fun selectDevice(deviceId: String) {
        _selectedDeviceId.value = deviceId
    }

    fun clearSelection() {
        _selectedDeviceId.value = null
        resetPeak()
    }

    fun updateSignalData(rssi: Int, heading: Float) {
        if (rssi > Constants.RSSI_FLOOR) {
            if (rssi > _peakRssi.value) {
                _peakRssi.value = rssi
                _peakHeading.value = heading
            }
            val newPoint = SignalPoint(rssi, heading, System.currentTimeMillis())
            val currentHistory = _signalHistory.value.toMutableList()
            currentHistory.add(0, newPoint)
            if (currentHistory.size > Constants.SIGNAL_HISTORY_MAX) currentHistory.removeAt(currentHistory.size - 1)
            _signalHistory.value = currentHistory
        }
    }

    fun resetPeak() {
        _peakRssi.value = Constants.RSSI_FLOOR
        _peakHeading.value = 0f
        _signalHistory.value = emptyList()
    }

    fun refreshBondedDevices() {
        _bondedDevices.value = bleRepository.getBondedDevices()
    }

    fun getRssiFlow(targetAddress: String): Flow<Int> = bleRepository.getRssiFlow(targetAddress)
}

data class SignalPoint(val rssi: Int, val heading: Float, val timestamp: Long)
