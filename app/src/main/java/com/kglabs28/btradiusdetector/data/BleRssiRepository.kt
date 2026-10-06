package com.kglabs28.btradiusdetector.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.kglabs28.btradiusdetector.data.util.MovingAverageFilter
import com.kglabs28.btradiusdetector.domain.model.BluetoothDeviceModel
import com.kglabs28.btradiusdetector.utils.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class BleRssiRepository(private val context: Context) {
    private val bluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy { bluetoothManager.adapter }
    private val scanner by lazy { bluetoothAdapter?.bluetoothLeScanner }

    private val filter = MovingAverageFilter(Constants.RSSI_SMOOTHING_WINDOW)

    fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun getBondedDevices(): List<BluetoothDeviceModel> {
        if (!hasConnectPermission()) {
            return emptyList()
        }
        return try {
            bluetoothAdapter?.bondedDevices?.map { device ->
                BluetoothDeviceModel(
                    address = device.address,
                    name = runCatching { device.name }.getOrNull() ?: "Unknown Device",
                    deviceClass = runCatching { device.bluetoothClass?.majorDeviceClass }.getOrNull()
                        ?: BluetoothClass.Device.Major.UNCATEGORIZED,
                    minorDeviceClass = runCatching { device.bluetoothClass?.deviceClass }.getOrNull() ?: 0,
                    isConnected = isDeviceConnected(device)
                )
            } ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun getBondedDevicesFlow(): Flow<List<BluetoothDeviceModel>> = callbackFlow {
        trySend(getBondedDevices())

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                trySend(getBondedDevices())
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        try {
            context.registerReceiver(receiver, intentFilter)
        } catch (_: Exception) {
        }

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Public, address-based connection check — used by DisconnectCheckWorker
     * to re-verify a device's state after the debounce delay, without needing
     * a live BluetoothDevice reference at that point.
     */
    @SuppressLint("MissingPermission")
    fun isConnected(address: String): Boolean {
        if (!hasConnectPermission()) return false
        return try {
            val device = bluetoothAdapter?.bondedDevices?.find { it.address == address } ?: return false
            isDeviceConnected(device)
        } catch (_: SecurityException) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun isDeviceConnected(device: BluetoothDevice): Boolean {
        if (!hasConnectPermission()) return false
        val profiles = intArrayOf(
            BluetoothProfile.GATT,
            BluetoothProfile.A2DP,
            BluetoothProfile.HEADSET
        )
        return profiles.any { profile ->
            try {
                bluetoothManager.getConnectionState(device, profile) == BluetoothProfile.STATE_CONNECTED
            } catch (_: Exception) {
                false
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun getRssiFlow(targetDeviceAddress: String): Flow<Int> = callbackFlow {
        if (!hasScanPermission()) {
            close(Exception("Bluetooth Scan permission not granted."))
            return@callbackFlow
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (result.device.address == targetDeviceAddress) {
                    trySend(filter.add(result.rssi).toInt())
                }
            }

            override fun onScanFailed(errorCode: Int) {
                close(Exception("Scan failed with error code: $errorCode"))
            }
        }

        val currentScanner = scanner
        if (currentScanner == null) {
            close(Exception("BLE Scanner not available. Check if Bluetooth is on."))
            return@callbackFlow
        }

        try {
            currentScanner.startScan(null, settings, callback)
        } catch (e: SecurityException) {
            close(e)
            return@callbackFlow
        }

        awaitClose {
            try {
                currentScanner.stopScan(callback)
            } catch (_: Exception) {
            }
            filter.clear()
        }
    }
}