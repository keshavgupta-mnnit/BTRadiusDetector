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
     * Merged RSSI stream from three sources, fastest-wins:
     *
     * 1. GATT live-link polling (primary while connected): `readRemoteRssi()`
     *    measures the actual link even when the buds stop advertising — the
     *    connected-but-silent case that BLE-only scanning can never see.
     * 2. BLE scan (advertising buds, nearby unpaired devices).
     * 3. Classic discovery, gated: inquiry starves BLE reception, so it runs
     *    only while no other source has produced a reading recently.
     *
     * Recovery rules: per-collector smoothing filter (no shared state), fresh
     * scanner lookup per start (no stale lazy), transient scan failures retry
     * instead of closing the flow, and BT on/off restarts the pipeline.
     */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION") // 4-arg connectGatt works on minSdk 24–37; executor variant is S+ only.
    fun getRssiFlow(targetDeviceAddress: String): Flow<Int> = callbackFlow {
        if (!hasScanPermission() || !hasConnectPermission()) {
            close(Exception("Bluetooth and Location permissions are required for signal tracking."))
            return@callbackFlow
        }
        val filter = MovingAverageFilter(Constants.RSSI_SMOOTHING_WINDOW)
        val scope = this
        val lastEmit = java.util.concurrent.atomic.AtomicLong(android.os.SystemClock.elapsedRealtime())

        fun emitRssi(rssi: Int) {
            lastEmit.set(android.os.SystemClock.elapsedRealtime())
            trySend(filter.add(rssi).toInt())
        }

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
                        emitRssi(result.rssi)
                    }
                }

                override fun onBatchScanResults(results: List<ScanResult>) {
                    results.lastOrNull { it.device.address == targetDeviceAddress }?.let {
                        emitRssi(it.rssi)
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

        // ---- GATT live link: polls the connected link's RSSI directly. ----
        var gatt: android.bluetooth.BluetoothGatt? = null
        var pollJob: kotlinx.coroutines.Job? = null
        var gattRetry: kotlinx.coroutines.Job? = null

        fun stopGatt() {
            pollJob?.cancel()
            pollJob = null
            gattRetry?.cancel()
            gattRetry = null
            runCatching {
                gatt?.disconnect()
                gatt?.close()
            }
            gatt = null
        }

        val gattCallback = object : android.bluetooth.BluetoothGattCallback() {
            override fun onConnectionStateChange(
                g: android.bluetooth.BluetoothGatt,
                status: Int,
                newState: Int
            ) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    pollJob?.cancel()
                    pollJob = scope.launch {
                        while (true) {
                            kotlinx.coroutines.delay(Constants.GATT_POLL_MS)
                            runCatching { g.readRemoteRssi() }
                        }
                    }
                } else {
                    pollJob?.cancel()
                    pollJob = null
                    // Single re-try while still collected; the stale supervisor
                    // covers the gap with discovery in the meantime.
                    gattRetry?.cancel()
                    gattRetry = scope.launch {
                        kotlinx.coroutines.delay(Constants.GATT_RETRY_MS)
                        if (bluetoothAdapter?.isEnabled == true) {
                            runCatching { g.connect() }
                        }
                    }
                }
            }

            override fun onReadRemoteRssi(g: android.bluetooth.BluetoothGatt, rssi: Int, status: Int) {
                if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                    emitRssi(rssi)
                }
            }
        }

        fun startGatt() {
            val adapter = bluetoothAdapter ?: return
            if (!adapter.isEnabled) return
            if (gatt != null) return
            val device = runCatching {
                adapter.bondedDevices?.find { it.address == targetDeviceAddress }
                    ?: adapter.getRemoteDevice(targetDeviceAddress)
            }.getOrNull() ?: return
            runCatching {
                gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            }
        }

        fun startDiscovery() {
            val adapter = bluetoothAdapter ?: return
            if (!adapter.isEnabled || adapter.isDiscovering) return
            runCatching { adapter.startDiscovery() }
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
                                emitRssi(rssi.toInt())
                            }
                        }
                    }
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                            BluetoothAdapter.STATE_ON -> {
                                filter.clear()
                                lastEmit.set(android.os.SystemClock.elapsedRealtime())
                                startBleScan()
                                startGatt()
                            }
                            BluetoothAdapter.STATE_OFF -> {
                                stopBleScan()
                                stopGatt()
                            }
                        }
                    }
                }
            }
        }

        val rssiFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        runCatching { context.registerReceiver(receiver, rssiFilter) }

        startBleScan()
        startGatt()

        // Stale supervisor: classic inquiry degrades BLE reception, so only
        // run it while nothing else has produced a reading recently.
        val supervisor = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(Constants.STALE_CHECK_MS)
                val adapter = bluetoothAdapter
                val stale = android.os.SystemClock.elapsedRealtime() - lastEmit.get() >
                    Constants.STALE_TIMEOUT_MS
                if (stale && adapter?.isEnabled == true && !adapter.isDiscovering) {
                    startDiscovery()
                }
            }
        }

        awaitClose {
            supervisor.cancel()
            stopBleScan()
            stopGatt()
            runCatching { bluetoothAdapter?.cancelDiscovery() }
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    /**
     * Best-effort headset battery level (0–100) for [targetDeviceAddress}.
     * Emits null when the device/ROM doesn't report it — many Buds-style
     * devices only expose battery via their companion app, never over HFP.
     * The UI hides the battery row in that case instead of showing stale data.
     *
     * Source: the (hidden, vendor-populated) `BATTERY_LEVEL_CHANGED` broadcast
     * plus its sticky intent for an instant first value. String literals are
     * used because the action/extra are @hide with no SDK constant.
     */
    @SuppressLint("MissingPermission")
    fun getBatteryFlow(targetDeviceAddress: String): Flow<Int?> = callbackFlow {
        if (!hasConnectPermission()) {
            close(Exception("Bluetooth Connect permission not granted."))
            return@callbackFlow
        }

        fun levelOf(intent: Intent?): Int? {
            if (intent == null) return null
            val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }
            if (device?.address != targetDeviceAddress) return null
            val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, BATTERY_UNKNOWN)
            return if (level in 0..100) level else null
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == ACTION_BATTERY_LEVEL_CHANGED) {
                    levelOf(intent)?.let { trySend(it) }
                }
            }
        }
        val filter = IntentFilter(ACTION_BATTERY_LEVEL_CHANGED)
        // Sticky lookup first: instant value when the ROM already broadcast it.
        val sticky = runCatching { context.registerReceiver(null, filter) }.getOrNull()
        levelOf(sticky)?.let { trySend(it) }
        runCatching { context.registerReceiver(receiver, filter) }

        awaitClose { runCatching { context.unregisterReceiver(receiver) } }
    }

    companion object {
        private const val ACTION_BATTERY_LEVEL_CHANGED =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        private const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
        private const val BATTERY_UNKNOWN = -1
    }
}