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
import com.kglabs28.btradiusdetector.data.DisconnectIntentStore
import com.kglabs28.btradiusdetector.domain.usecase.EnsureDisconnectAlertUseCase
import com.kglabs28.btradiusdetector.service.MonitoringService
import com.kglabs28.btradiusdetector.utils.Constants
import com.kglabs28.btradiusdetector.utils.NotificationUtils
import com.kglabs28.btradiusdetector.workers.ConnectEventWorker
import com.kglabs28.btradiusdetector.workers.DisconnectCheckWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Alert entry point. The drop flag is written synchronously here so a later
 * process death can't lose it; posting runs off-main via goAsync, and the
 * debounced worker only verifies/retracts afterwards.
 */
class BluetoothAlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Reboot: bring the monitor back without waiting for app launch.
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            MonitoringService.ensure(context)
            return
        }
        val device = extractDevice(intent) ?: return
        val address = device.address
        val workManager = WorkManager.getInstance(context)

        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> onConnected(context, workManager, address)
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                DisconnectIntentStore(context).markDisconnected(address)
                onDisconnected(context, workManager, address)
            }

            // Profile-level fallbacks: on some ROMs the ACL broadcast is
            // throttled while profile state changes still arrive (and vice
            // versa). The verifier retracts anything that wasn't a real drop.
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                when (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)) {
                    BluetoothProfile.STATE_CONNECTED -> onConnected(context, workManager, address)
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        DisconnectIntentStore(context).markDisconnected(address)
                        onDisconnected(context, workManager, address)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun onConnected(context: Context, workManager: WorkManager, address: String) {
        Log.d(TAG, "connected: $address — clearing any disconnect alert")
        DisconnectIntentStore(context).clear(address)
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

    private fun onDisconnected(context: Context, workManager: WorkManager, address: String) {
        // goAsync: the DB + binder reads below must not run on the main thread.
        // The flag above already guarantees delivery even if this dies.
        val pending = goAsync()
        receiverScope.launch {
            try {
                MonitoringService.ensure(context)
                EnsureDisconnectAlertUseCase.create(context).ensure(address)
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
