# Project Plan

Build an Android app called BTRadiusDetector in Kotlin using Jetpack Compose. 
App purpose: Help a user locate a nearby paired Bluetooth device by walking toward the direction where Bluetooth signal strength (RSSI) is highest, using body rotation to estimate a rough bearing. Also monitor paired devices and notify the user on connect/disconnect (range alerts).

Core Features:
1. Device selection screen: List bonded devices with connection status and generic icons.
2. Main tracking screen: 
   - Sonar-style radar UI.
   - Smooth RSSI dot (moving average filter).
   - Compass ring (Rotation Vector sensor).
   - "Best signal" direction tracking.
   - Signal strength text (Cold/Warm/Hot).
   - Instruction/onboarding overlay (first launch).
   - Watch companion trigger (UI stub/disabled).
3. Settings screen: Toggles for range-alert monitoring per device.
4. Notifications: Foreground service to monitor ACL CONNECT/DISCONNECT. Fire notifications on state change.

Technical constraints:
- RSSI polling via ScanCallback (BLE).
- DataStore for persistence.
- Clean Architecture (Repositories + ViewModels).
- Dark navy/sonar theme with cyan/green glowing accents.
- Exclude: Battery %, "buzzing" earbuds, precise AoA.
- Permissions: BLUETOOTH_SCAN, BLUETOOTH_CONNECT, ACCESS_FINE_LOCATION, FOREGROUND_SERVICE, POST_NOTIFICATIONS.

## Project Brief

The project brief for BTRadiusDetector has been generated. It includes a sonar-style radar tracking feature, background range alerts via a foreground service, and a modern tech stack using Jetpack Navigation 3 and Compose Material Adaptive.

## Implementation Steps

### Task_1_ProjectFoundation: Set up project foundation, including dependencies, permissions in AndroidManifest, DataStore for persistence, and basic navigation structure.
- **Status:** COMPLETED
- **Updates:** - Added Bluetooth and notification permissions to AndroidManifest.xml.
- **Acceptance Criteria:**
  - AndroidManifest includes BLUETOOTH_SCAN, BLUETOOTH_CONNECT, ACCESS_FINE_LOCATION, POST_NOTIFICATIONS, and FOREGROUND_SERVICE permissions.
  - DataStore is initialized for storing onboarding flags and device-specific alert settings.
  - Navigation structure with Compose is set up.

### Task_2_RepositoriesAndModels: Implement core logic layers: BleRssiRepository for scanning and RSSI smoothing, and CompassRepository for sensor fusion.
- **Status:** COMPLETED
- **Updates:** - Implemented `MovingAverageFilter` for signal smoothing.
- **Acceptance Criteria:**
  - BleRssiRepository correctly handles BLE scans and provides a smoothed RSSI stream.
  - CompassRepository correctly maps Rotation Vector sensor data to magnetic/true heading.

### Task_3_DeviceSelectionScreen: Build the Device Selection screen showing bonded devices with their connection status and category icons.
- **Status:** COMPLETED
- **Updates:** - Created `DeviceSelectionScreen` with a list of bonded devices.
- **Acceptance Criteria:**
  - Bonded devices are correctly retrieved and displayed.
  - Icons are mapped based on Bluetooth Class of Device.
  - Navigation to the tracking screen is functional.

### Task_4_TrackingScreenUI: Implement the Main Tracking screen UI, including the sonar-style radar, pulsing center dot, and compass ring.
- **Status:** COMPLETED
- **Updates:** - Implemented `SonarRadar` component with concentric circles and grid lines.
- **Acceptance Criteria:**
  - Sonar-style radar UI matches the provided design (dark navy/cyan accents).
  - Pulsing dot reacts to smoothed RSSI values.
  - Compass ring rotates according to device orientation.

### Task_5_TrackingLogic: Connect tracking UI to logic: display peak signal markers, distance category labels (Cold/Warm/Hot), and the onboarding overlay.
- **Status:** COMPLETED
- **Updates:** - Defined RSSI thresholds in `RssiConstants.kt`.
- **Acceptance Criteria:**
  - Peak signal direction is correctly tracked and displayed.
  - Signal strength labels update based on defined thresholds.
  - Onboarding overlay is shown on first launch and hidden afterwards.

### Task_6_BackgroundMonitoring: Implement the Foreground Service and BroadcastReceiver to monitor device connect/disconnect events and show notifications.
- **Status:** COMPLETED
- **Updates:** - Implemented `MonitoringService` as a foreground service with notification channel setup.
- **Acceptance Criteria:**
  - Foreground service runs persistently with a low-priority notification.
  - ACL_CONNECTED/DISCONNECTED events trigger the correct transition notifications.
  - Monitoring state respects the per-device settings in DataStore.

### Task_7_SettingsScreen: Build the Settings screen with per-device alert toggles and UI stubs for Wear OS.
- **Status:** COMPLETED
- **Updates:** - Created `SettingsScreen` with range alert toggles for each bonded device.
- **Acceptance Criteria:**
  - Settings screen displays a list of bonded devices with toggle switches.
  - Toggles correctly update DataStore.
  - Wear OS trigger button is present but disabled as a stub.

### Task_8_FinalVerification: Perform final integration testing and UI refinement.
- **Status:** COMPLETED
- **Updates:** - Performed static code review of all core modules.
- Verified `AndroidManifest.xml` includes all 5 required permissions and `MonitoringService` declaration.
- Confirmed `SonarTheme` consistency across `DeviceSelectionScreen`, `DetailsScreen`, and `SettingsScreen`.
- Validated `MovingAverageFilter` and `CompassRepository` logic through unit tests and code inspection.
- Successfully ran final build and unit tests (`assembleDebug`, `testDebugUnitTest`).
- Project is ready for deployment.
- **Acceptance Criteria:**
  - All core features (Discovery, Tracking, Monitoring, Settings) are functional.
  - UI matches the design reference.
  - Permissions are handled gracefully.
- **Duration:** N/A

