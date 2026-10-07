package com.kglabs28.btradiusdetector.service

import android.app.Service
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.kglabs28.btradiusdetector.data.AlertSettingsRepository
import com.kglabs28.btradiusdetector.data.BleRssiRepository
import com.kglabs28.btradiusdetector.data.local.AppDatabase
import com.kglabs28.btradiusdetector.domain.usecase.EnsureDisconnectAlertUseCase
import com.kglabs28.btradiusdetector.utils.Constants
import com.kglabs28.btradiusdetector.utils.NotificationUtils
import com.kglabs28.btradiusdetector.utils.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Always-on monitor: a foreground service (with the "Monitoring" ongoing
 * notification from the design) owns a live receiver, so disconnects alert
 * instantly even when the app process was dead — no dependency on deferred
 * WorkManager timing. The manifest receiver + workers remain as fallback;
 * stable notification IDs dedupe the two paths.
 */
class MonitoringService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val verifyJobs = mutableMapOf<String, Job>()
    private var receiver: BroadcastReceiver? = null

    private val settingsRepository by lazy {
        AlertSettingsRepository(AppDatabase.getInstance(this).alertSettingsDao())
    }
    private val bleRepository by lazy { BleRssiRepository(this) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(ACTION_BATTERY_LEVEL_CHANGED)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_ACL_CONNECTED -> deviceAddressOf(intent)?.let { onConnected(it) }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> deviceAddressOf(intent)?.let { onDisconnected(it) }
                    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                    BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                        val address = deviceAddressOf(intent) ?: return
                        when (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)) {
                            BluetoothProfile.STATE_CONNECTED -> onConnected(address)
                            BluetoothProfile.STATE_DISCONNECTED -> onDisconnected(address)
                            else -> Unit
                        }
                    }
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                            stopSelf()
                        } else {
                            scope.launch { refreshOngoing() }
                        }
                    }
                    ACTION_BATTERY_LEVEL_CHANGED -> scope.launch { refreshOngoing() }
                }
            }
        }
        this.receiver = receiver
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Foreground contract first: startForegroundService() demands
        // startForeground() before this call returns to the system — even if
        // we then immediately stand down. Skipping it (e.g. stopping when
        // idle without foregrounding) kills the process. So: foreground now
        // with a placeholder, decide in the coroutine, update or stop.
        try {
            startForegroundWithType(NotificationUtils.buildMonitoringNotification(
                this, null, Strings.monitoringTitle, Strings.monitoringBodyConnected
            ))
        } catch (e: SecurityException) {
            // Permissions revoked between ensure() and here — never crash-loop.
            Log.w(TAG, "foreground start denied, stopping", e)
            stopSelf()
            return START_NOT_STICKY
        }
        scope.launch {
            val live = liveMonitoredDevice()
            if (live == null) {
                // Nothing live left to watch — clear the shade and stand down.
                // The manifest receiver restarts us on the next connect.
                NotificationUtils.cancelSafely(this@MonitoringService, Constants.NOTIF_ID_MONITORING)
                stopSelf()
                return@launch
            }
            val battery = bleRepository.getLastKnownBattery(live.address)
            NotificationUtils.notifySafely(
                this@MonitoringService,
                Constants.NOTIF_ID_MONITORING,
                NotificationUtils.buildMonitoringNotification(
                    this@MonitoringService,
                    live.address,
                    Strings.monitoringTitleFor(live.name ?: live.address),
                    battery?.let { Strings.monitoringBodyConnectedWithBattery(it) }
                        ?: Strings.monitoringBodyConnected
                )
            )
            EnsureDisconnectAlertUseCase.create(this@MonitoringService).reconcilePending()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        receiver?.let { runCatching { unregisterReceiver(it) } }
        receiver = null
        verifyJobs.values.forEach { it.cancel() }
        verifyJobs.clear()
        scope.cancel()
        super.onDestroy()
    }

    private fun onConnected(address: String) {
        verifyJobs.remove(address)?.cancel()
        NotificationUtils.cancelSafely(this, NotificationUtils.disconnectNotificationId(address))
        com.kglabs28.btradiusdetector.data.DisconnectIntentStore(this).clear(address)
        scope.launch {
            val settings = settingsRepository.getByAddress(address) ?: return@launch
            if (settings.monitoringEnabled && settings.notifyOnReconnect) {
                val name = bondedName(address)
                val battery = bleRepository.getLastKnownBattery(address)
                NotificationUtils.notifySafely(
                    this@MonitoringService,
                    Constants.NOTIF_ID_RECONNECT_BASE + address.hashCode(),
                    NotificationUtils.buildReconnectNotification(
                        this@MonitoringService, address,
                        Strings.reconnectTitle(name),
                        battery?.let { Strings.reconnectBodyWithBattery(it) } ?: Strings.reconnectBody,
                        settings.soundEnabled, settings.vibrationEnabled
                    )
                )
            }
            refreshOngoing()
        }
    }

    private fun onDisconnected(address: String) {
        com.kglabs28.btradiusdetector.data.DisconnectIntentStore(this).markDisconnected(address)
        scope.launch {
            EnsureDisconnectAlertUseCase.create(this@MonitoringService).ensure(address)
            verifyJobs.remove(address)?.cancel()
            verifyJobs[address] = launch {
                delay(Constants.DISCONNECT_DEBOUNCE_MS)
                if (bleRepository.isConnected(address)) {
                    NotificationUtils.cancelSafely(
                        this@MonitoringService,
                        NotificationUtils.disconnectNotificationId(address)
                    )
                    Log.d(TAG, "blip retracted: $address")
                }
                refreshOngoing()
            }
            refreshOngoing()
        }
    }

    private suspend fun hasLiveMonitoredDevice(): Boolean = liveMonitoredDevice() != null

    /**
     * Refreshes the ongoing row while something is live; clears the shade
     * and stands down the moment nothing is left to watch.
     */
    private suspend fun refreshOngoing() {
        val live = liveMonitoredDevice()
        if (live == null) {
            NotificationUtils.cancelSafely(this, Constants.NOTIF_ID_MONITORING)
            stopSelf()
            return
        }
        val battery = bleRepository.getLastKnownBattery(live.address)
        NotificationUtils.notifySafely(
            this,
            Constants.NOTIF_ID_MONITORING,
            NotificationUtils.buildMonitoringNotification(
                this,
                live.address,
                Strings.monitoringTitleFor(live.name ?: live.address),
                battery?.let { Strings.monitoringBodyConnectedWithBattery(it) }
                    ?: Strings.monitoringBodyConnected
            )
        )
    }

    private suspend fun liveMonitoredDevice(): com.kglabs28.btradiusdetector.domain.model.BluetoothDeviceModel? {
        val devices = runCatching { bleRepository.getBondedDevices() }.getOrDefault(emptyList())
        val rows = runCatching { settingsRepository.monitoringMap() }.getOrDefault(emptyMap())
        // Ongoing shade presence is explicit opt-in: only devices the user
        // actually enabled in Range Alerts (a stored row toggled on).
        // One-shot connect/disconnect alerts keep working for everyone.
        val monitored = devices.filter { rows[it.address] == true }
        return monitored.firstOrNull { it.isConnected }
    }

    private fun startForegroundWithType(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                Constants.NOTIF_ID_MONITORING,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(Constants.NOTIF_ID_MONITORING, notification)
        }
    }

    private fun bondedName(address: String): String =
        runCatching { bleRepository.getBondedDevices().find { it.address == address }?.name }.getOrNull()
            ?: address

    private fun deviceAddressOf(intent: Intent): String? {
        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
        return device?.address
    }

    companion object {
        private const val TAG = "MonitoringService"
        private const val ACTION_BATTERY_LEVEL_CHANGED =
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"

        /**
         * Best-effort start; no-ops without BLUETOOTH_CONNECT (notably on
         * first launch before the permission gate passes) because Android 16
         * refuses the connectedDevice foreground type and would crash-loop.
         * Background-start denials fall back to the manifest path.
         */
        fun ensure(context: Context) {
            if (!BleRssiRepository(context).hasConnectPermission()) return
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, MonitoringService::class.java))
            }
        }
    }
}
