package com.kglabs28.btradiusdetector.util

object Constants {
    const val RSSI_HOT = -60
    const val RSSI_WARM = -80
    // Cold is anything below -80

    const val NOTIFICATION_CHANNEL_ID = "monitoring_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Bluetooth Monitoring"
    const val FOREGROUND_NOTIFICATION_ID = 1
    const val TRANSITION_NOTIFICATION_ID = 2
}

enum class DistanceCategory(val label: String) {
    HOT("Hot"),
    WARM("Warm"),
    COLD("Cold"),
    UNKNOWN("Searching...")
}

fun getDistanceCategory(rssi: Int): DistanceCategory {
    return when {
        rssi >= Constants.RSSI_HOT -> DistanceCategory.HOT
        rssi >= Constants.RSSI_WARM -> DistanceCategory.WARM
        rssi < Constants.RSSI_WARM && rssi > -100 -> DistanceCategory.COLD
        else -> DistanceCategory.UNKNOWN
    }
}
