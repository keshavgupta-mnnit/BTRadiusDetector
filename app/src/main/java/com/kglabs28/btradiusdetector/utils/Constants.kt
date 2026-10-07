package com.kglabs28.btradiusdetector.utils

object Constants {
    // Signal strength thresholds (dBm)
    const val RSSI_HOT = -60
    const val RSSI_WARM = -80
    const val RSSI_FLOOR = -100

    // Notification channels
    const val CHANNEL_ID_SILENT = "range_alerts_silent"
    const val CHANNEL_ID_SOUND = "range_alerts_sound"
    const val CHANNEL_ID_SOUND_NO_VIB = "range_alerts_sound_novib"
    const val CHANNEL_ID_VIBRATE = "range_alerts_vibrate"

    // Notification IDs
    const val NOTIF_ID_DISCONNECT_BASE = 1000
    const val NOTIF_ID_RECONNECT_BASE = 2000
    const val NOTIF_ID_BEEPING = 4000

    // Beep loop tuning (no magic numbers in the service)
    const val BEEP_VIBRATE_INTERVAL_MS = 2_000L
    const val BEEP_VIBRATE_DURATION_MS = 400L

    // Behavior tuning
    const val RSSI_SMOOTHING_WINDOW = 5

    // Tracking UI tuning (no magic numbers in composables)
    const val SIGNAL_SEGMENTS = 20
    const val SIGNAL_HISTORY_MAX = 20
    const val RADAR_RINGS = 4
    const val RADAR_SWEEP_DURATION_MS = 5_000
    const val DOT_PULSE_DURATION_MS = 1_200
    const val COMPASS_TICK_STEP_DEG = 6
    const val COMPASS_LABEL_STEP_DEG = 30
    const val BEST_WEDGE_SWEEP_DEG = 35f
    const val BEST_WEDGE_OFFSET_DEG = 17.5f
    const val RSSI_RANGE_SPAN = 70
    const val DOT_SCALE_BASE = 0.5f
    const val DOT_SCALE_RANGE = 0.5f

    // RSSI pipeline recovery (no magic numbers in repository)
    const val SCAN_RETRY_DELAY_MS = 3_000L
    const val NEARBY_SNAPSHOT_MS = 1_000L
    const val GUIDANCE_DEAD_ZONE_DEG = 10f

    // Live-link RSSI polling (no magic numbers in repository)
    const val GATT_POLL_MS = 2_000L
    const val GATT_RETRY_MS = 5_000L
    const val GATT_BATTERY_RETRY_MS = 30_000L
    const val STALE_CHECK_MS = 5_000L
    const val STALE_TIMEOUT_MS = 10_000L

    // Pending disconnect flags older than this never alert (stale drop).
    const val PENDING_ALERT_STALE_MS = 30 * 60 * 1_000L

    // Notification history caps (no magic numbers in the store).
    const val HISTORY_PER_DEVICE = 10
    const val HISTORY_MAX_TOTAL = 200

    // Same-device same-event posts inside this window are one physical
    // flap (ACL + A2DP + headset broadcasts for a single connect) — post once.
    const val ALERT_DEDUP_MS = 5_000L

    // Background glow (no magic numbers in composables)
    const val RADIAL_GLOW_RADIUS = 1000f

    // DataStore
    const val PREFS_NAME = "user_preferences"
    const val KEY_SHOW_ONBOARDING = "show_onboarding"
}

enum class DistanceCategory(val label: String) {
    HOT("Hot"), WARM("Warm"), COLD("Cold"), UNKNOWN("Searching...")
}

fun getDistanceCategory(rssi: Int): DistanceCategory {
    return when {
        rssi >= Constants.RSSI_HOT -> DistanceCategory.HOT
        rssi >= Constants.RSSI_WARM -> DistanceCategory.WARM
        rssi > Constants.RSSI_FLOOR -> DistanceCategory.COLD
        else -> DistanceCategory.UNKNOWN
    }
}