package com.kglabs28.btradiusdetector.ui

import androidx.lifecycle.ViewModel
import com.kglabs28.btradiusdetector.data.BleRssiRepository
import com.kglabs28.btradiusdetector.data.CompassRepository
import com.kglabs28.btradiusdetector.domain.model.BluetoothDeviceModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(
    private val bleRepository: BleRssiRepository,
    private val compassRepository: CompassRepository
) : ViewModel() {

    private val _bondedDevices = MutableStateFlow<List<BluetoothDeviceModel>>(emptyList())
    val bondedDevices: StateFlow<List<BluetoothDeviceModel>> = _bondedDevices.asStateFlow()

    private val _peakRssi = MutableStateFlow(-100)
    val peakRssi = _peakRssi.asStateFlow()

    private val _peakHeading = MutableStateFlow(0f)
    val peakHeading = _peakHeading.asStateFlow()

    private val _signalHistory = MutableStateFlow<List<SignalPoint>>(emptyList())
    val signalHistory = _signalHistory.asStateFlow()

    val headingFlow: Flow<Float> = compassRepository.getHeadingFlow()

    fun updateSignalData(rssi: Int, heading: Float) {
        if (rssi > -100) {
            if (rssi > _peakRssi.value) {
                _peakRssi.value = rssi
                _peakHeading.value = heading
            }
            val newPoint = SignalPoint(rssi, heading, System.currentTimeMillis())
            val currentHistory = _signalHistory.value.toMutableList()
            currentHistory.add(0, newPoint)
            if (currentHistory.size > 20) currentHistory.removeAt(currentHistory.size - 1)
            _signalHistory.value = currentHistory
        }
    }

    fun resetPeak() {
        _peakRssi.value = -100
        _peakHeading.value = 0f
        _signalHistory.value = emptyList()
    }

    fun refreshBondedDevices() {
        _bondedDevices.value = bleRepository.getBondedDevices()
    }

    fun getRssiFlow(targetAddress: String): Flow<Int> = bleRepository.getRssiFlow(targetAddress)
}

data class SignalPoint(val rssi: Int, val heading: Float, val timestamp: Long)