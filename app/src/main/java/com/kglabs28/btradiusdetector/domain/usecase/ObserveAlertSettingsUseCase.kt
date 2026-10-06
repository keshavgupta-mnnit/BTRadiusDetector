package com.kglabs28.btradiusdetector.domain.usecase

import com.kglabs28.btradiusdetector.data.AlertSettingsRepository
import com.kglabs28.btradiusdetector.data.local.AlertSettingsEntity
import kotlinx.coroutines.flow.Flow

/** Read side of per-device alert preferences. */
class ObserveAlertSettingsUseCase(private val repository: AlertSettingsRepository) {

    fun observeAll(): Flow<List<AlertSettingsEntity>> = repository.observeAll()

    fun observeByAddress(address: String): Flow<AlertSettingsEntity?> =
        repository.observeByAddress(address)

    suspend fun getByAddress(address: String): AlertSettingsEntity? =
        repository.getByAddress(address)
}
