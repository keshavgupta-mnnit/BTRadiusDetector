package com.kglabs28.btradiusdetector.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
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
import kotlinx.coroutines.launch

class BleRssiRepository(private val context: Context) {
    private val bluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy { bluetoothManager.adapter }

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
        val scanOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        // Classic discovery RSSI is location-derived on every API level.
        val locationOk = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return scanOk && locationOk
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
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
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
        // 1) Profile-level checks. BluetoothManager.getConnectionState() is only
        // reliable for GATT; classic profiles (A2DP/HEADSET) must be queried via
        // the adapter so earbuds/speakers/watches are not missed.
        if (isConnectedViaProfiles(device)) return true
        // 2) ACL-level fallback via hidden BluetoothDevice.isConnected().
        // Catches HID/PAN/MAP/LE-audio links that expose no public profile API.
        return isConnectedViaReflection(device)
    }

    @SuppressLint("MissingPermission")
    private fun isConnectedViaProfiles(device: BluetoothDevice): Boolean {
        val address = device.address
        val profiles = buildList {
            add(BluetoothProfile.GATT)
            add(BluetoothProfile.GATT_SERVER)
            add(BluetoothProfile.A2DP)
            add(BluetoothProfile.HEADSET)
            add(BluetoothProfile.HEALTH)
            // NOTE: HID/PAN/MAP/SAP/A2DP_SINK proxy constants are absent from
            // this compile SDK's BluetoothProfile, so they are covered by the
            // ACL-level reflection fallback in isConnectedViaReflection() below.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) add(BluetoothProfile.HEARING_AID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(BluetoothProfile.LE_AUDIO)
        }
        return profiles.any { profile ->
            // Per-device connected-device list (accurate, no cross-device false positives).
            val inConnectedSet = runCatching {
                bluetoothManager.getConnectedDevices(profile).any { it.address == address }
            }.getOrDefault(false)
            if (inConnectedSet) return@any true
            // Per-device state as second signal (works for GATT; classic proxies may throw).
            runCatching {
                bluetoothManager.getConnectionState(device, profile) == BluetoothProfile.STATE_CONNECTED
            }.getOrDefault(false)
        }
    }

    private fun isConnectedViaReflection(device: BluetoothDevice): Boolean {
        return runCatching {
            val method = device.javaClass.getMethod("isConnected")
            method.isAccessible = true
            method.invoke(device) as? Boolean ?: false
        }.getOrDefault(false)
    }

    /**
     * Merged RSSI stream: BLE scan (primary) + classic discovery (fallback).
     *
     * Why both: connected classic devices (A2DP earbuds/speakers) often stop LE
     * advertising, so a BLE-only scan goes silent while still connected — the
     * exact "full strength, then nothing, never recovers" symptom. Classic
     * ACTION_FOUND carries EXTRA_RSSI and sees those devices.
     *
     * Recovery rules: per-collector smoothing filter (no shared state), fresh
     * scanner lookup per start (no stale lazy), transient scan failures retry
     * instead of closing the flow, and BT on/off restarts the pipeline.
     */
    @SuppressLint("MissingPermission")
    fun getRssiFlow(targetDeviceAddress: String): Flow<Int> = callbackFlow {
        if (!hasScanPermission()) {
            close(Exception("Bluetooth Scan permission not granted."))
            return@callbackFlow
        }
        val filter = MovingAverageFilter(Constants.RSSI_SMOOTHING_WINDOW)
        val scope = this
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        var bleCallback: ScanCallback? = null

        fun startBleScan() {
            if (bleCallback != null) return
            val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    if (result.device.address == targetDeviceAddress) {
                        trySend(filter.add(result.rssi).toInt())
                    }
                }

                override fun onBatchScanResults(results: List<ScanResult>) {
                    results.lastOrNull { it.device.address == targetDeviceAddress }?.let {
                        trySend(filter.add(it.rssi).toInt())
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    // Transient (e.g. BT restart, registration loss): retry, never kill the flow.
                    bleCallback = null
                    scope.launch {
                        kotlinx.coroutines.delay(Constants.SCAN_RETRY_DELAY_MS)
                        startBleScan()
                    }
                }
            }
            runCatching { scanner.startScan(null, settings, callback) }
                .onSuccess { bleCallback = callback }
                .onFailure {
                    scope.launch {
                        kotlinx.coroutines.delay(Constants.SCAN_RETRY_DELAY_MS)
                        startBleScan()
                    }
                }
        }

        fun stopBleScan() {
            bleCallback?.let { callback ->
                runCatching { bluetoothAdapter?.bluetoothLeScanner?.stopScan(callback) }
            }
            bleCallback = null
        }

        var discoveryRestart: kotlinx.coroutines.Job? = null

        fun startDiscovery() {
            val adapter = bluetoothAdapter ?: return
            if (!adapter.isEnabled) return
            runCatching {
                if (adapter.isDiscovering) adapter.cancelDiscovery()
                adapter.startDiscovery()
            }
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        if (device?.address == targetDeviceAddress) {
                            val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                            if (rssi != Short.MIN_VALUE) {
                                trySend(filter.add(rssi.toInt()).toInt())
                            }
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        // Discovery is single-shot (~12s): re-arm while collected.
                        discoveryRestart?.cancel()
                        discoveryRestart = scope.launch {
                            kotlinx.coroutines.delay(Constants.DISCOVERY_RESTART_GAP_MS)
                            startDiscovery()
                        }
                    }
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                            BluetoothAdapter.STATE_ON -> {
                                filter.clear()
                                startBleScan()
                                startDiscovery()
                            }
                            BluetoothAdapter.STATE_OFF -> stopBleScan()
                        }
                    }
                }
            }
        }

        val rssiFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        runCatching { context.registerReceiver(receiver, rssiFilter) }

        startBleScan()
        startDiscovery()

        awaitClose {
            discoveryRestart?.cancel()
            stopBleScan()
            runCatching { bluetoothAdapter?.cancelDiscovery() }
            runCatching { context.unregisterReceiver(receiver) }
        }
    }
}