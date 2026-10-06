package com.kglabs28.btradiusdetector.domain.usecase

import com.kglabs28.btradiusdetector.data.BleRssiRepository
import kotlinx.coroutines.flow.Flow

/** Smoothed live RSSI for one address (GATT poll + BLE scan + gated discovery). */
class ObserveRssiUseCase(private val bleRepository: BleRssiRepository) {

    operator fun invoke(address: String): Flow<Int> =
        bleRepository.getRssiFlow(address)
}
