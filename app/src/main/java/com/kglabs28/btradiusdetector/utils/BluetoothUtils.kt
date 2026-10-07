package com.kglabs28.btradiusdetector.utils

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
import com.kglabs28.btradiusdetector.domain.model.NearbyDevice
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * Direct Bluetooth system access. Plain functions taking [Context] — no
 * instances, no stored state, no ceremony. Pure data maintenance (the alert
 * flags table) lives in AlertSettingsRepository instead.
 */
object BluetoothUtils {

    private val TAG = LogUtils.tag("BleScan")

    // Google Fast Pair service: buds able to show system battery cards
    // advertise here. We log its raw bytes (debug only) to decode per-bud
    // levels instead of guessing the vendor format.
    private val FAST_PAIR_UUID =
        java.util.UUID.fromString("0000fe2c-0000-1000-8000-00805f9a34fb")

    private fun bluetoothManager(context: Context): BluetoothManager =
        context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private fun bluetoothAdapter(context: Context): BluetoothAdapter? =
        bluetoothManager(context).adapter

    fun hasConnectPermission(context: Context): Boolean {
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

    fun hasScanPermission(context: Context): Boolean {
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
    fun getBondedDevices(context: Context): List<BluetoothDeviceModel> {
        if (!hasConnectPermission(context)) {
            return emptyList()
        }
        return try {
            bluetoothAdapter(context)?.bondedDevices?.map { device ->
                BluetoothDeviceModel(
                    address = device.address,
                    name = runCatching { device.name }.getOrNull() ?: "Unknown Device",
                    deviceClass = runCatching { device.bluetoothClass?.majorDeviceClass }.getOrNull()
                        ?: BluetoothClass.Device.Major.UNCATEGORIZED,
                    minorDeviceClass = runCatching { device.bluetoothClass?.deviceClass }.getOrNull() ?: 0,
                    isConnected = isDeviceConnected(context, device)
                )
            } ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun getBondedDevicesFlow(context: Context): Flow<List<BluetoothDeviceModel>> = callbackFlow {
        trySend(getBondedDevices(context))

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                trySend(getBondedDevices(context))
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
     * Address-based connection check for background alert paths that hold no
     * live BluetoothDevice reference.
     */
    @SuppressLint("MissingPermission")
    fun isConnected(context: Context, address: String): Boolean {
        if (!hasConnectPermission(context)) return false
        return try {
            val device = bluetoothAdapter(context)?.bondedDevices?.find { it.address == address } ?: return false
            isDeviceConnected(context, device)
        } catch (_: SecurityException) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun isDeviceConnected(context: Context, device: BluetoothDevice): Boolean {
        if (!hasConnectPermission(context)) return false
        if (isConnectedViaProfiles(context, device)) return true
        // ACL-level fallback via hidden BluetoothDevice.isConnected().
        return isConnectedViaReflection(device)
    }

    @SuppressLint("MissingPermission")
    private fun isConnectedViaProfiles(context: Context, device: BluetoothDevice): Boolean {
        val address = device.address
        val manager = bluetoothManager(context)
        val profiles = buildList {
            add(BluetoothProfile.GATT)
            add(BluetoothProfile.GATT_SERVER)
            add(BluetoothProfile.A2DP)
            add(BluetoothProfile.HEADSET)
            add(BluetoothProfile.HEALTH)
            // NOTE: HID/PAN/MAP/SAP/A2DP_SINK proxy constants are absent from
            // this compile SDK's BluetoothProfile, so they are covered by the
            // ACL-level reflection fallback below.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) add(BluetoothProfile.HEARING_AID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(BluetoothProfile.LE_AUDIO)
        }
        return profiles.any { profile ->
            // Per-device connected-device list (accurate, no cross-device false positives).
            val inConnectedSet = runCatching {
                manager.getConnectedDevices(profile).any { it.address == address }
            }.getOrDefault(false)
            if (inConnectedSet) return@any true
            // Per-device state as second signal (works for GATT; classic proxies may throw).
            runCatching {
                manager.getConnectionState(device, profile) == BluetoothProfile.STATE_CONNECTED
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

    // Debug only: dump the Fast Pair service bytes so per-bud battery formats
    // can be decoded from a real device instead of guessed.
    private fun logFastPairBytes(result: ScanResult) {
        val record = result.scanRecord ?: return
        val uuids = record.serviceUuids?.joinToString(",") { it.uuid.toString() } ?: "none"
        val fastPair = record.getServiceData(android.os.ParcelUuid(FAST_PAIR_UUID))
        val hex = fastPair?.joinToString("") { "%02X".format(it) } ?: "absent"
        LogUtils.d(TAG, "adv rssi=${result.rssi} services=[$uuids] fastpair=$hex")
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
    fun getRssiFlow(context: Context, targetDeviceAddress: String): Flow<Int> = callbackFlow {
        if (!hasScanPermission(context) || !hasConnectPermission(context)) {
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
            val scanner = bluetoothAdapter(context)?.bluetoothLeScanner ?: return
            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    if (result.device.address == targetDeviceAddress) {
                        emitRssi(result.rssi)
                        logFastPairBytes(result)
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
                runCatching { bluetoothAdapter(context)?.bluetoothLeScanner?.stopScan(callback) }
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
                        if (bluetoothAdapter(context)?.isEnabled == true) {
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
            val adapter = bluetoothAdapter(context) ?: return
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
            val adapter = bluetoothAdapter(context) ?: return
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
                val adapter = bluetoothAdapter(context)
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
            runCatching { bluetoothAdapter(context)?.cancelDiscovery() }
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    /**
     * Best-effort headset battery level (0–100) for [targetDeviceAddress].
     * Emits null when the device/ROM doesn't report it — many Buds-style
     * devices only expose battery via their companion app, never over HFP.
     * The UI hides the battery row in that case instead of showing stale data.
     *
     * Source: the (hidden, vendor-populated) `BATTERY_LEVEL_CHANGED` broadcast
     * plus its sticky intent for an instant first value. String literals are
     * used because the action/extra are @hide with no SDK constant.
     */
    @SuppressLint("MissingPermission")
    fun getBatteryFlow(context: Context, targetDeviceAddress: String): Flow<Int?> = callbackFlow {
        if (!hasConnectPermission(context)) {
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

        // GATT Battery Service path: many buds never send the HFP broadcast
        // but do expose the standard BAS (0x180F) over LE — read it directly.
        val scope = this
        var basGatt: android.bluetooth.BluetoothGatt? = null
        var basPoll: kotlinx.coroutines.Job? = null

        fun readBasLevel(g: android.bluetooth.BluetoothGatt) {
            val characteristic = g.getService(BAS_SERVICE_UUID)?.getCharacteristic(BAS_LEVEL_UUID)
                ?: return
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    g.readCharacteristic(characteristic)
                } else {
                    @Suppress("DEPRECATION")
                    g.readCharacteristic(characteristic)
                }
            }
        }

        fun stopBas() {
            basPoll?.cancel()
            basPoll = null
            runCatching {
                basGatt?.disconnect()
                basGatt?.close()
            }
            basGatt = null
        }

        val basCallback = object : android.bluetooth.BluetoothGattCallback() {
            override fun onConnectionStateChange(
                g: android.bluetooth.BluetoothGatt,
                status: Int,
                newState: Int
            ) {
                if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                    runCatching { g.discoverServices() }
                } else {
                    basPoll?.cancel()
                    basPoll = null
                }
            }

            override fun onServicesDiscovered(g: android.bluetooth.BluetoothGatt, status: Int) {
                if (status != android.bluetooth.BluetoothGatt.GATT_SUCCESS) return
                // Debug only: full service table — looking for extra battery
                // instances or vendor characteristics carrying per-bud levels.
                val table = g.services?.joinToString(";") { service ->
                    val chars = service.characteristics?.joinToString(",") { it.uuid.toString() } ?: ""
                    "${service.uuid}[$chars]"
                } ?: "none"
                LogUtils.d(TAG, "gatt services=$table")
                readBasLevel(g)
                basPoll?.cancel()
                basPoll = scope.launch {
                    while (true) {
                        kotlinx.coroutines.delay(GATT_BATTERY_POLL_MS)
                        readBasLevel(g)
                    }
                }
            }

            @Suppress("DEPRECATION")
            override fun onCharacteristicRead(
                g: android.bluetooth.BluetoothGatt,
                characteristic: android.bluetooth.BluetoothGattCharacteristic,
                status: Int
            ) {
                if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS &&
                    characteristic.uuid == BAS_LEVEL_UUID
                ) {
                    characteristic.value?.firstOrNull()?.let { trySend(it.toInt() and 0xFF) }
                }
            }

            @androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
            override fun onCharacteristicRead(
                g: android.bluetooth.BluetoothGatt,
                characteristic: android.bluetooth.BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS &&
                    characteristic.uuid == BAS_LEVEL_UUID
                ) {
                    value.firstOrNull()?.let { trySend(it.toInt() and 0xFF) }
                }
            }
        }

        runCatching {
            val adapter = bluetoothAdapter(context)
            if (adapter?.isEnabled == true && basGatt == null) {
                val device = adapter.bondedDevices?.find { it.address == targetDeviceAddress }
                    ?: adapter.getRemoteDevice(targetDeviceAddress)
                basGatt = device.connectGatt(context, false, basCallback, BluetoothDevice.TRANSPORT_LE)
            }
        }

        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
            stopBas()
        }
    }

    /**
     * Synchronous sticky-broadcast lookup for notification bodies.
     * Returns null when nothing was ever broadcast for [address].
     */
    fun getLastKnownBattery(context: Context, address: String): Int? {
        if (!hasConnectPermission(context)) return null
        val sticky = runCatching {
            context.registerReceiver(null, IntentFilter(ACTION_BATTERY_LEVEL_CHANGED))
        }.getOrNull() ?: return null
        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            sticky.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            sticky.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
        if (device?.address != address) return null
        val level = sticky.getIntExtra(EXTRA_BATTERY_LEVEL, BATTERY_UNKNOWN)
        return if (level in 0..100) level else null
    }

    // ---- Nearby (unfiltered) BLE scan: every overheard radio ----

    /**
     * All nearby advertisers, bonded or not — the single-bud finder. Each
     * disconnected bud advertises under its own address, which usually
     * differs from the bonded pair address, so filtering by bond would hide
     * exactly the bud you're looking for. Emits a snapshot ~1/sec, newest
     * strongest first, entries older than [STALE_TIMEOUT_MS] pruned.
     */
    @SuppressLint("MissingPermission")
    fun getNearbyBleFlow(context: Context): Flow<List<NearbyDevice>> = callbackFlow {
        if (!hasScanPermission(context)) {
            close(Exception("Bluetooth Scan permission not granted."))
            return@callbackFlow
        }
        val scope = this
        val seen = mutableMapOf<String, NearbyDevice>()

        fun snapshot(): List<NearbyDevice> {
            val cutoff = android.os.SystemClock.elapsedRealtime() - Constants.STALE_TIMEOUT_MS
            seen.entries.removeAll { it.value.lastSeenMillis < cutoff }
            return seen.values.sortedByDescending { it.rssi }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        var bleCallback: ScanCallback? = null

        fun startBleScan() {
            if (bleCallback != null) return
            val scanner = bluetoothAdapter(context)?.bluetoothLeScanner ?: return
            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val address = result.device.address ?: return
                    seen[address] = NearbyDevice(
                        address = address,
                        name = runCatching { result.device.name }.getOrNull()
                            ?: result.scanRecord?.deviceName,
                        rssi = result.rssi,
                        lastSeenMillis = android.os.SystemClock.elapsedRealtime()
                    )
                }

                override fun onBatchScanResults(results: List<ScanResult>) {
                    results.forEach { result ->
                        val address = result.device.address ?: return@forEach
                        seen[address] = NearbyDevice(
                            address = address,
                            name = runCatching { result.device.name }.getOrNull()
                                ?: result.scanRecord?.deviceName,
                            rssi = result.rssi,
                            lastSeenMillis = android.os.SystemClock.elapsedRealtime()
                        )
                    }
                }

                override fun onScanFailed(errorCode: Int) {
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
                runCatching { bluetoothAdapter(context)?.bluetoothLeScanner?.stopScan(callback) }
            }
            bleCallback = null
        }

        startBleScan()
        // Snapshot ticker: collectors get a fresh sorted list periodically.
        val ticker = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(Constants.NEARBY_SNAPSHOT_MS)
                trySend(snapshot())
            }
        }

        awaitClose {
            ticker.cancel()
            stopBleScan()
        }
    }

    // GATT Battery Service (0x180F) / Battery Level (0x2A19) — standard SIG UUIDs.
    private const val ACTION_BATTERY_LEVEL_CHANGED =
        "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
    private const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"
    private const val BATTERY_UNKNOWN = -1
    private val BAS_SERVICE_UUID =
        java.util.UUID.fromString("0000180f-0000-1000-8000-00805f9a34fb")
    private val BAS_LEVEL_UUID =
        java.util.UUID.fromString("00002a19-0000-1000-8000-00805f9a34fb")
    private const val GATT_BATTERY_POLL_MS = 60_000L
}
