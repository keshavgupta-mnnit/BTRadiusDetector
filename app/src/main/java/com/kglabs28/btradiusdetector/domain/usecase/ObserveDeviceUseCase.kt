package com.kglabs28.btradiusdetector.domain.usecase

import com.kglabs28.btradiusdetector.domain.model.BluetoothDeviceModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Reactive single-device lookup. Filtering lives here (UDF) —
 * never in @Composable via remember { devices.find {} }.
 */
class ObserveDeviceUseCase(private val observeBondedDevices: ObserveBondedDevicesUseCase) {

    operator fun invoke(address: String): Flow<BluetoothDeviceModel?> =
        observeBondedDevices().map { devices -> devices.find { it.address == address } }
}
