package com.kglabs28.btradiusdetector.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kglabs28.btradiusdetector.data.BleRssiRepository
import com.kglabs28.btradiusdetector.data.CompassRepository
import com.kglabs28.btradiusdetector.data.local.UserPreferencesRepository
import com.kglabs28.btradiusdetector.domain.model.BluetoothDeviceModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val bleRepository: BleRssiRepository,
    private val compassRepository: CompassRepository
) : ViewModel() {
    val userPreferences = preferencesRepository.userPreferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

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
            
            // Add to history and keep last 20 points
            val newPoint = SignalPoint(rssi, heading, System.currentTimeMillis())
            val currentHistory = _signalHistory.value.toMutableList()
            currentHistory.add(0, newPoint)
            if (currentHistory.size > 20) {
                currentHistory.removeAt(currentHistory.size - 1)
            }
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

    fun getRssiFlow(targetAddress: String): Flow<Int> {
        return bleRepository.getRssiFlow(targetAddress)
    }

    fun toggleMonitoring(address: String, enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.toggleDeviceMonitoring(address, enabled)
        }
    }

    suspend fun updateOnboardingCompleted() {
        preferencesRepository.updateShowOnboarding(false)
    }
}

data class SignalPoint(val rssi: Int, val heading: Float, val timestamp: Long)
