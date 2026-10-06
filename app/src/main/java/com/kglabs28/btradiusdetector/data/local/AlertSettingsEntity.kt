package com.kglabs28.btradiusdetector.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kglabs28.btradiusdetector.utils.Constants

@Entity(tableName = "alert_settings")
data class AlertSettingsEntity(
    @PrimaryKey val address: String,
    val monitoringEnabled: Boolean = true,
    val notifyOnDisconnect: Boolean = false,
    val notifyOnReconnect: Boolean = false,
    val soundEnabled: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val keepNotifyingOnDisconnect: Boolean = false,
    val lastBestRssi: Int = Constants.RSSI_FLOOR,
    val lastBestHeading: Float = 0f
)