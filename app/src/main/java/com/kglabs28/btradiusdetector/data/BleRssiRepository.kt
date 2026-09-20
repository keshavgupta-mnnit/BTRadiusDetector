package com.kglabs28.btradiusdetector.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import com.kglabs28.btradiusdetector.data.util.MovingAverageFilter
import com.kglabs28.btradiusdetector.domain.model.BluetoothDeviceModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Repository for handling BLE scanning and providing smoothed RSSI values.
 */
class BleRssiRepository(private val context: Context) {
    private val bluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }
    private val scanner by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }

    private val filter = MovingAverageFilter(5)

    /**
     * Returns a list of bonded (paired) devices.
     */
    @SuppressLint("MissingPermission")
    fun getBondedDevices(): List<BluetoothDeviceModel> {
        return bluetoothAdapter?.bondedDevices?.map { device ->
            BluetoothDeviceModel(
                address = device.address,
                name = device.name ?: "Unknown Device",
                deviceClass = device.bluetoothClass?.majorDeviceClass ?: BluetoothClass.Device.Major.UNCATEGORIZED,
                isConnected = isDeviceConnected(device)
            )
        } ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    private fun isDeviceConnected(device: BluetoothDevice): Boolean {
        // Only use GATT for checking connection status as per user request to avoid profile unsupported exceptions
        return bluetoothManager.getConnectionState(device, BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED
    }

    /**
     * Returns a Flow of smoothed RSSI values for the specified target device.
     */
    @SuppressLint("MissingPermission")
    fun getRssiFlow(targetDeviceAddress: String): Flow<Int> = callbackFlow {
        // We avoid using ScanFilter with setDeviceAddress on the scanner level 
        // because it can be unreliable on some Android versions/devices, 
        // especially for bonded devices using RPAs.
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (result.device.address == targetDeviceAddress) {
                    val smoothedRssi = filter.add(result.rssi).toInt()
                    trySend(smoothedRssi)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                close(Exception("Scan failed with error code: $errorCode"))
            }
        }

        if (scanner == null) {
            close(Exception("BLE Scanner not available. Check if Bluetooth is on."))
            return@callbackFlow
        }

        // Start scan without filter and filter in onScanResult
        scanner?.startScan(null, settings, callback)

        awaitClose {
            scanner?.stopScan(callback)
            filter.clear()
        }
    }
}
