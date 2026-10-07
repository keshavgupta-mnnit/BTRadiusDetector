package com.kglabs28.btradiusdetector.domain.model

/** One overheard radio: any BLE advertiser, bonded or not. */
data class NearbyDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val lastSeenMillis: Long = System.currentTimeMillis()
)
