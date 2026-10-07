package com.kglabs28.btradiusdetector.domain.usecase

import com.kglabs28.btradiusdetector.data.AlertSettingsRepository
import com.kglabs28.btradiusdetector.domain.model.AlertRepeatMode

/** Write side of per-device alert preferences. One method per toggle. */
class UpdateAlertSettingUseCase(private val repository: AlertSettingsRepository) {

    suspend fun setMonitoring(address: String, enabled: Boolean) =
        repository.setMonitoringEnabled(address, enabled)

    suspend fun setNotifyOnDisconnect(address: String, enabled: Boolean) =
        repository.setNotifyOnDisconnect(address, enabled)

    suspend fun setNotifyOnReconnect(address: String, enabled: Boolean) =
        repository.setNotifyOnReconnect(address, enabled)

    suspend fun setSound(address: String, enabled: Boolean) =
        repository.setSoundEnabled(address, enabled)

    suspend fun setRepeatMode(address: String, mode: AlertRepeatMode) =
        repository.setAlertRepeat(address, mode)

    suspend fun setVibration(address: String, enabled: Boolean) =
        repository.setVibrationEnabled(address, enabled)
}
