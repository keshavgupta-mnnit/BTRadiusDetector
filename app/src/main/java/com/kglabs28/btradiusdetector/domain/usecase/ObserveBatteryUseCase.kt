package com.kglabs28.btradiusdetector.domain.usecase

import com.kglabs28.btradiusdetector.data.BleRssiRepository
import kotlinx.coroutines.flow.Flow

/** Best-effort headset battery; null when the device never reports it. */
class ObserveBatteryUseCase(private val bleRepository: BleRssiRepository) {

    operator fun invoke(address: String): Flow<Int?> =
        bleRepository.getBatteryFlow(address)
}
