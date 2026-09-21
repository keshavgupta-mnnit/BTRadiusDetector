package com.kglabs28.btradiusdetector.utils

object Strings {
    // Home
    const val homeTitle = "BTRadiusDetector"
    const val pairedDevicesHeader = "PAIRED DEVICES"
    const val noBondedDevices = "No Bonded Devices Found"
    const val refresh = "REFRESH"
    const val monitorRangeAlertsTitle = "Monitor for range alerts"
    const val monitorRangeAlertsSubtitle = "Get notified when devices go out of range or reconnect."

    // Permissions
    const val permissionsRequiredTitle = "Permissions Required"
    const val permissionsRequiredBody =
        "Bluetooth and Location permissions are needed to find and track your devices. Notifications are used for range alerts."
    const val grantPermissions = "GRANT PERMISSIONS"
    const val continueLabel = "Continue"
    const val notNow = "Not Now"

    // Tracking
    const val signalStrength = "SIGNAL STRENGTH"
    const val currentHeading = "CURRENT HEADING"
    const val searching = "Searching..."
    const val bestSignal = "Best signal"

    // Onboarding / instructions
    const val onboardingTitle = "Welcome to BTRadiusDetector"
    const val gotIt = "GOT IT"

    // Settings
    const val settingsTitle = "Settings"
    const val rangeAlertsTitle = "Range Alerts"
    const val rangeAlertsSubtitle = "Get notified when a monitored device goes out of range or reconnects."
    const val aboutTitle = "About"
    const val aboutBody = "BTRadiusDetector helps you find nearby paired Bluetooth devices using signal strength and heading."

    // Range Alerts Details
    const val notifyOnDisconnect = "Notify on disconnect"
    const val notifyOnReconnect = "Notify on reconnect"
    const val notifyWithSound = "Notify with sound & vibration"
    const val keepNotifyingOnDisconnect = "Keep notifying until dismissed"
    const val keepNotifyingSubtitle = "Repeats the disconnect alert every few seconds until you tap it."

    // Notifications
    fun disconnectTitle(deviceName: String) = "$deviceName disconnected"
    const val disconnectBody = "Device may be out of range."
    fun reconnectTitle(deviceName: String) = "$deviceName reconnected"
    const val reconnectBody = "Connection restored."

    // Disconnect info screen
    fun disconnectedAgo(deviceName: String, elapsed: String) = "$deviceName disconnected $elapsed"
    const val disconnectHint = "Retrace your last few steps to find it."
}