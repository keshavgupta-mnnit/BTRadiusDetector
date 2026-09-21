package com.kglabs28.btradiusdetector.utils

object Constants {
    // Signal strength thresholds (dBm)
    const val RSSI_HOT = -60
    const val RSSI_WARM = -80
    const val RSSI_FLOOR = -100 // anything at/below this is treated as no signal

    // Notification channels
    const val CHANNEL_ID_SILENT = "range_alerts_silent"
    const val CHANNEL_ID_SOUND = "range_alerts_sound"
    const val CHANNEL_ID_STICKY = "sticky_disconnect_alert"

    // Notification IDs
    const val NOTIF_ID_DISCONNECT_BASE = 1000 // offset per-device by address hashCode
    const val NOTIF_ID_RECONNECT_BASE = 2000
    const val NOTIF_ID_STICKY = 3000

    // Behavior tuning
    const val DISCONNECT_DEBOUNCE_MS = 15_000L // ignore a disconnect if reconnect follows within this window
    const val STICKY_REPEAT_INTERVAL_MS = 5_000L

    const val RSSI_SMOOTHING_WINDOW = 5
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
        rssi > Constants.RSSI_FLOOR -> DistanceCategory.COLD
        else -> DistanceCategory.UNKNOWN
    }
}