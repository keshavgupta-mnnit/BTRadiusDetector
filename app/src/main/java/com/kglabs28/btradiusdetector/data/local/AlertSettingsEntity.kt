package com.kglabs28.btradiusdetector.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alert_settings")
data class AlertSettingsEntity(
    @PrimaryKey val address: String,
    val notifyOnDisconnect: Boolean = false,
    val notifyOnReconnect: Boolean = false,
    val soundEnabled: Boolean = false,
    val keepNotifyingOnDisconnect: Boolean = false
)