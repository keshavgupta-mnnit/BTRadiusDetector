package com.kglabs28.btradiusdetector.data

import com.kglabs28.btradiusdetector.data.local.AlertSettingsDao
import com.kglabs28.btradiusdetector.data.local.AlertSettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Plain CRUD over one flags row per device. Reads go straight to the DAO —
 * no in-memory copies, no whole-table scans for a single row.
 */
class AlertSettingsRepository(private val dao: AlertSettingsDao) {

    fun observeAll(): Flow<List<AlertSettingsEntity>> = dao.observeAll()

    fun observeByAddress(address: String): Flow<AlertSettingsEntity?> =
        dao.observeByAddress(address)

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

    /**
     * Passive tracking must not opt the device into alerts: the row it
     * creates stays master-off (sub-toggles default on, inert).
     */
    suspend fun saveBestDirection(address: String, rssi: Int, heading: Float) {
        val current = dao.getByAddress(address)
        if (current == null) {
            dao.upsert(
                AlertSettingsEntity(
                    address = address,
                    monitoringEnabled = false,
                    lastBestRssi = rssi,
                    lastBestHeading = heading
                )
            )
        } else {
            dao.upsert(current.copy(lastBestRssi = rssi, lastBestHeading = heading))
        }
    }

    private suspend inline fun upsertCopy(
        address: String,
        transform: AlertSettingsEntity.() -> AlertSettingsEntity
    ) {
        val current = dao.getByAddress(address) ?: AlertSettingsEntity(address = address)
        dao.upsert(current.transform())
    }
}
