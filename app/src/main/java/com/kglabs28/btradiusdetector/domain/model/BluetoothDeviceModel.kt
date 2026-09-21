package com.kglabs28.btradiusdetector.domain.model

data class BluetoothDeviceModel(
    val address: String,
    val name: String?,
    val deviceClass: Int,
    val minorDeviceClass: Int,
    val isConnected: Boolean
)