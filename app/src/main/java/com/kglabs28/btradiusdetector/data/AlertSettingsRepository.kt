package com.kglabs28.btradiusdetector.data

import com.kglabs28.btradiusdetector.data.local.AlertSettingsDao
import com.kglabs28.btradiusdetector.data.local.AlertSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single point of access for per-device alert preferences and the persisted
 * best-signal snapshot. Workers, ViewModels and services all go through here
 * instead of touching the DAO directly.
 */
class AlertSettingsRepository(private val dao: AlertSettingsDao) {

    fun observeAll(): Flow<List<AlertSettingsEntity>> = dao.observeAll()

    fun observeByAddress(address: String): Flow<AlertSettingsEntity?> =
        dao.observeAll().map { list -> list.find { it.address == address } }

    suspend fun getByAddress(address: String): AlertSettingsEntity? =
        dao.getByAddress(address)

    suspend fun setMonitoringEnabled(address: String, enabled: Boolean) =
        upsertCopy(address) { copy(monitoringEnabled = enabled) }

    suspend fun setNotifyOnDisconnect(address: String, enabled: Boolean) =
        upsertCopy(address) { copy(notifyOnDisconnect = enabled) }

    suspend fun setNotifyOnReconnect(address: String, enabled: Boolean) =
        upsertCopy(address) { copy(notifyOnReconnect = enabled) }

    suspend fun setSoundEnabled(address: String, enabled: Boolean) =
        upsertCopy(address) { copy(soundEnabled = enabled) }

    suspend fun setVibrationEnabled(address: String, enabled: Boolean) =
        upsertCopy(address) { copy(vibrationEnabled = enabled) }

    suspend fun saveBestDirection(address: String, rssi: Int, heading: Float) =
        upsertCopy(address) { copy(lastBestRssi = rssi, lastBestHeading = heading) }

    private suspend inline fun upsertCopy(
        address: String,
        transform: AlertSettingsEntity.() -> AlertSettingsEntity
    ) {
        val current = dao.getByAddress(address) ?: AlertSettingsEntity(address = address)
        dao.upsert(current.transform())
    }
}
