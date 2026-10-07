package com.kglabs28.btradiusdetector.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kglabs28.btradiusdetector.utils.Constants

import com.kglabs28.btradiusdetector.domain.model.AlertSoundMode

@Entity(tableName = "alert_settings")
data class AlertSettingsEntity(
    @PrimaryKey val address: String,
    val monitoringEnabled: Boolean = true,
    val notifyOnDisconnect: Boolean = true,
    val notifyOnReconnect: Boolean = true,
    val soundMode: String = AlertSoundMode.ONCE.name,
    val vibrationEnabled: Boolean = true,
    val keepNotifyingOnDisconnect: Boolean = false,
    val lastBestRssi: Int = Constants.RSSI_FLOOR,
    val lastBestHeading: Float = 0f
)