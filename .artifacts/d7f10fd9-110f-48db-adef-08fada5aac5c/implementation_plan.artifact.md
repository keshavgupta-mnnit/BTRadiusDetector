# Debug Tracking and Update UI for Tracking Screen

The goal is to fix the "unable to track" issue by improving the `BleRssiRepository` and to update the `TrackingScreen` UI to include a signal strength bar, a heading card, and sonar markers as per the reference description.

## User Review Required

> [!IMPORTANT]
> The tracking issue might be caused by the device being already connected or being a Classic Bluetooth device. `BluetoothLeScanner` only finds advertising BLE devices. I will attempt to improve this by ensuring the scan settings are optimal and suggesting GATT-based RSSI for connected devices if applicable.

## Proposed Changes

### Data Layer

#### [MODIFY] [BleRssiRepository.kt](file:///C:/Users/Keshav/Documents/MyDocs/AP/BTRadiusDetector/app/src/main/java/com/kglabs28/btradiusdetector/data/BleRssiRepository.kt)
- Update `getRssiFlow` to use a more robust scanning approach.
- Consider removing the `ScanFilter` or making it more lenient if it's blocking results.
- Add logic to handle cases where the scanner might not be returning results (e.g., check if device is connected).

### UI Layer

#### [MODIFY] [TrackingScreen.kt](file:///C:/Users/Keshav/Documents/MyDocs/AP/BTRadiusDetector/app/src/main/java/com/kglabs28/btradiusdetector/ui/screens/TrackingScreen.kt)
- **Heading Card**: Add a stylized card at the top displaying the numerical heading.
- **Signal Strength Bar**: Add a visual signal bar (Progress Indicator) to the information column.
- **Sonar Markers**: Add small "blips" on the radar view to represent signal history or peak signal locations.
- **Improved Radar**: Enhance the `RadarView` with more expressive animations.

## Verification Plan

### Automated Tests
- Build the project to ensure no syntax errors.
- Check logs for any scanning failures.

### Manual Verification
- Verify the new UI elements are visible in the `@Preview`.
- Test tracking with a real BLE device to see if RSSI updates are received.
