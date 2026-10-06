package com.kglabs28.btradiusdetector.receiver

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kglabs28.btradiusdetector.data.BleRssiRepository
import com.kglabs28.btradiusdetector.data.local.AlertSettingsEntity
import com.kglabs28.btradiusdetector.data.local.AppDatabase
import com.kglabs28.btradiusdetector.service.StickyAlertService
import com.kglabs28.btradiusdetector.utils.Constants
import com.kglabs28.btradiusdetector.utils.NotificationUtils
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.workers.ConnectEventWorker
import com.kglabs28.btradiusdetector.workers.DisconnectCheckWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Alert entry point. Disconnects notify *immediately* here; the debounced
 * [DisconnectCheckWorker] only verifies afterwards and retracts blips.
 * Nothing user-visible ever waits on WorkManager scheduling.
 */
class BluetoothAlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val device = extractDevice(intent) ?: return
        val address = device.address
        val workManager = WorkManager.getInstance(context)

        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> onConnected(context, workManager, address)
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> onDisconnected(context, workManager, address, authoritative = true)

            // Profile-level fallbacks: on some ROMs the ACL broadcast is
            // throttled while profile state changes still arrive (and vice
            // versa). Profile drops are advisory only — the link may be alive.
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                when (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)) {
                    BluetoothProfile.STATE_CONNECTED -> onConnected(context, workManager, address)
                    BluetoothProfile.STATE_DISCONNECTED ->
                        onDisconnected(context, workManager, address, authoritative = false)
                    else -> Unit
                }
            }
        }
    }

    private fun onConnected(context: Context, workManager: WorkManager, address: String) {
        Log.d(TAG, "connected: $address — clearing any disconnect alert")
        // A reconnect cancels any pending verify pass and retracts an
        // immediate alert if the drop turned out to be a blip.
        workManager.cancelUniqueWork(disconnectWorkName(address))
        NotificationUtils.cancelSafely(context, NotificationUtils.disconnectNotificationId(address))

        val request = OneTimeWorkRequestBuilder<ConnectEventWorker>()
            .setInputData(ConnectEventWorker.buildInput(address))
            .build()
        workManager.enqueueUniqueWork(
            connectWorkName(address),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun onDisconnected(
        context: Context,
        workManager: WorkManager,
        address: String,
        authoritative: Boolean
    ) {
        // goAsync: the DB + binder reads below must not run on the main thread.
        val pending = goAsync()
        receiverScope.launch {
            try {
                postDisconnectAlert(context.applicationContext, address, authoritative)
            } finally {
                pending.finish()
            }
        }

        // Verification pass after the debounce window: retracts blips,
        // re-posts if the immediate post never ran.
        val request = OneTimeWorkRequestBuilder<DisconnectCheckWorker>()
            .setInputData(DisconnectCheckWorker.buildInput(address))
            .setInitialDelay(Constants.DISCONNECT_DEBOUNCE_MS, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(
            disconnectWorkName(address),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private suspend fun postDisconnectAlert(appContext: Context, address: String, authoritative: Boolean) {
        val dao = AppDatabase.getInstance(appContext).alertSettingsDao()
        val settings = dao.getByAddress(address) ?: AlertSettingsEntity(address = address)
        if (!settings.monitoringEnabled || !settings.notifyOnDisconnect) {
            Log.d(TAG, "disconnect alert disabled, skipping: $address")
            return
        }
        val repo = BleRssiRepository(appContext)
        if (!authoritative && repo.isConnected(address)) {
            Log.d(TAG, "profile blip while link alive, no alert: $address")
            return
        }
        val deviceName = repo.getBondedDevices().find { it.address == address }?.name ?: address
        if (settings.keepNotifyingOnDisconnect) {
            StickyAlertService.start(appContext, address, deviceName)
        } else {
            NotificationUtils.notifySafely(
                appContext,
                NotificationUtils.disconnectNotificationId(address),
                NotificationUtils.buildDisconnectNotification(
                    appContext,
                    address,
                    Strings.disconnectTitle(deviceName),
                    NotificationUtils.disconnectBody(repo, settings, address),
                    settings.soundEnabled,
                    settings.vibrationEnabled
                )
            )
        }
        Log.d(TAG, "disconnect alert posted immediately (authoritative=$authoritative): $address")
    }

    private fun extractDevice(intent: Intent): BluetoothDevice? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    companion object {
        private const val TAG = "BluetoothAlertReceiver"
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private fun disconnectWorkName(address: String) = "disconnect_check_$address"
        private fun connectWorkName(address: String) = "connect_event_$address"
    }
}
