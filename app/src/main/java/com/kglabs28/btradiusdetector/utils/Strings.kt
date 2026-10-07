package com.kglabs28.btradiusdetector.utils

object Strings {
    // Home
    const val homeTitle = "BTRadiusDetector"
    const val pairedDevicesHeader = "Paired Devices"
    const val noBondedDevices = "No Bonded Devices Found"
    const val noPairedDevicesShort = "No paired devices found."
    const val refresh = "REFRESH"
    const val monitorRangeAlertsTitle = "Monitor for range alerts"
    const val monitorRangeAlertsSubtitle = "Get notified when devices go out of range or reconnect."
    const val menuDesc = "Menu"
    const val settingsDesc = "Settings"
    const val backDesc = "Back"

    // Permissions
    const val permissionsRequiredTitle = "Permissions Required"
    const val permissionsRequiredBody =
        "Bluetooth and Location permissions are needed to find and track your devices. Notifications are used for range alerts."
    const val grantPermissions = "GRANT PERMISSIONS"
    const val continueLabel = "Continue"
    const val notNow = "Not Now"

    // Tracking
    const val signalStrength = "Signal strength"
    const val signalStrengthUpper = "SIGNAL STRENGTH"
    const val currentHeading = "Current heading"
    const val currentHeadingUpper = "CURRENT HEADING"
    const val searching = "Searching..."
    const val bestSignal = "Best signal"
    const val connected = "Connected"
    const val disconnected = "Disconnected"
    const val paired = "Paired"
    const val unknownDevice = "Unknown Device"
    const val howToFindDesc = "How to find"
    const val buzzMyWatch = "Buzz my watch"
    const val watchBuzzNeedsCompanion = "Watch buzz requires companion app"
    const val proximityStrong = "Strong"
    const val proximityMedium = "Medium"
    const val proximityWeak = "Weak"
    const val facingBestSignal = "Facing best signal — walk that way"
    fun turnLeft(degrees: Int) = "Turn $degrees° left"
    fun turnRight(degrees: Int) = "Turn $degrees° right"
    fun findingTitle(deviceName: String) = "Finding: $deviceName"
    fun bestSignalLabel(degrees: Int) = "Best signal\n($degrees°)"
    fun dbmLabel(rssi: Int) = "$rssi dBm"
    fun headingLabel(degrees: Int, cardinal: String) = "$degrees° $cardinal"

    // Onboarding / instructions
    const val onboardingTitle = "Welcome to BTRadiusDetector"
    const val howToFindTitle = "How to find your device"
    const val gotIt = "Got it"
    const val gotItUpper = "GOT IT"
    const val closeDesc = "Close"
    val howToSteps = listOf(
        "Stand still.",
        "Slowly turn in a full circle.",
        "Watch where the signal is strongest.",
        "Walk that way.",
        "Repeat."
    )

    // Settings
    const val settingsTitle = "Settings"
    const val rangeAlertsTitle = "Range Alerts"
    const val rangeAlertsSubtitle = "Get notified when devices go out of range or reconnect."
    const val otherSettingsTitle = "Other Settings"
    const val alertSettingsTitle = "Alert Settings"
    const val alertDetailsCaption = "Configure which devices to monitor and how to be notified."
    const val batteryTitle = "Battery"
    fun batteryPercent(percent: Int) = "$percent%"
    const val notificationSoundTitle = "Notification sound"
    const val notificationSoundDefault = "Default"
    const val notificationSoundAlt = "Chime"
    const val vibrationTitle = "Vibration"
    const val vibrationOn = "On"
    const val vibrationOff = "Off"
    const val aboutTitle = "About"
    const val aboutBody = "BTRadiusDetector helps you find nearby paired Bluetooth devices using signal strength and heading."

    // Range Alerts Details
    const val notifyOnDisconnect = "Notify on disconnect"
    const val notifyOnReconnect = "Notify on reconnect"
    const val notifyWithSound = "Notify with sound & vibration"
    const val keepNotifyingOnDisconnect = "Keep notifying until dismissed"
    const val keepNotifyingSubtitle = "Repeats the disconnect alert every few seconds until you tap it."

    // Notifications
    fun disconnectTitle(deviceName: String) = "$deviceName Connection Lost"
    const val disconnectBody = "Device may be out of range."
    fun disconnectBodyWithBattery(percent: Int) = "$disconnectBody Battery $percent%."
    fun reconnectTitle(deviceName: String) = "$deviceName Connected"
    const val reconnectBody = "Connection restored."
    fun reconnectBodyWithBattery(percent: Int) = "$reconnectBody Battery $percent%."

    // Disconnect info screen
    fun disconnectedAgo(deviceName: String, elapsed: String) = "$deviceName disconnected $elapsed"
    const val disconnectHint = "Retrace your last few steps to find it."
    fun lastSeenLabel(degrees: Int, cardinal: String) = "Last seen $degrees° $cardinal."
}
