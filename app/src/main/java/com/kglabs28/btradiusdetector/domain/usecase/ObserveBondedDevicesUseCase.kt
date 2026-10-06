package com.kglabs28.btradiusdetector.domain.usecase

import com.kglabs28.btradiusdetector.data.BleRssiRepository
import com.kglabs28.btradiusdetector.domain.model.BluetoothDeviceModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Live bonded-device list. Owns the refresh trigger so screens and
 * ViewModels never touch the repository directly for this stream.
 */
class ObserveBondedDevicesUseCase(private val bleRepository: BleRssiRepository) {

    private val refreshTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<BluetoothDeviceModel>> =
        refreshTrigger.flatMapLatest { bleRepository.getBondedDevicesFlow() }

    fun refresh() {
        refreshTrigger.value += 1
    }
}
