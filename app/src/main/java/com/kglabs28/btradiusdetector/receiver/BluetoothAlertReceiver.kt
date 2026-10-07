package com.kglabs28.btradiusdetector.receiver

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kglabs28.btradiusdetector.MainActivity
import com.kglabs28.btradiusdetector.data.DisconnectIntentStore
import com.kglabs28.btradiusdetector.domain.usecase.EnsureAlertUseCase
import com.kglabs28.btradiusdetector.service.BeepService
import com.kglabs28.btradiusdetector.utils.BluetoothUtils
import com.kglabs28.btradiusdetector.utils.Constants
import com.kglabs28.btradiusdetector.utils.LogUtils
import com.kglabs28.btradiusdetector.utils.NotificationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The single alert path: Bluetooth connect/disconnect broadcast arrives
 * (even with the app dead — this receiver is manifest-declared), we pull the
 * device from the intent and hand it to [EnsureAlertUseCase], which checks
 * the registered toggles in the DB and notifies only when registered for
 * that exact event. Nothing else posts alerts.
 */
class BluetoothAlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Beep stop: user tapped Stop (or the alert itself) on a continuous beep.
        if (intent.action == NotificationUtils.ACTION_STOP_BEEPING) {
            val address = intent.getStringExtra(NotificationUtils.EXTRA_BEEP_ADDRESS) ?: return
            val name = bondedName(context, address)
            LogUtils.d(TAG, "beep stopped by user for $name")
            BeepService.stop(context)
            NotificationUtils.cancelSafely(context, Constants.NOTIF_ID_BEEPING)
            if (intent.getBooleanExtra(NotificationUtils.EXTRA_BEEP_OPEN_TRACKING, false)) {
                context.startActivity(
                    Intent(context, MainActivity::class.java).apply {
                        action = MainActivity.ACTION_TRACK_DEVICE
                        putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, address)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            return
        }
        val device = extractDevice(intent) ?: return
        val address = device.address
        val name = device.name ?: address
        val event = when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> "Connected"
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> "Disconnected"
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                when (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)) {
                    BluetoothProfile.STATE_CONNECTED -> "Connected"
                    BluetoothProfile.STATE_DISCONNECTED -> "Disconnected"
                    else -> null
                }
            }
            else -> null
        } ?: return
        LogUtils.d(TAG, "Broadcast Received - $event - $name")

        when (event) {
            "Connected" -> onConnected(context, address, name)
            else -> {
                DisconnectIntentStore(context).markDisconnected(address)
                onDisconnected(context, address, name)
            }
        }
    }

    private fun onConnected(context: Context, address: String, name: String) {
        // Reconnect ends everything for this device: stop its beep loop.
        BeepService.stopFor(context, address)
        // goAsync: DB + binder reads must not run on the main thread.
        val pending = goAsync()
        receiverScope.launch {
            try {
                DisconnectIntentStore(context).clear(address)
                NotificationUtils.cancelSafely(context, NotificationUtils.disconnectNotificationId(address))
                EnsureAlertUseCase.create().ensureConnect(address)
            } catch (e: Exception) {
                LogUtils.e(TAG, "connect alert failed for $name", e)
            } finally {
                pending.finish()
            }
        }
    }

    private fun onDisconnected(context: Context, address: String, name: String) {
        // goAsync: DB + binder reads must not run on the main thread.
        // The flag above already guarantees delivery even if this dies.
        val pending = goAsync()
        receiverScope.launch {
            try {
                EnsureAlertUseCase.create().ensureDisconnect(address)
            } catch (e: Exception) {
                LogUtils.e(TAG, "immediate post failed for $name", e)
            } finally {
                pending.finish()
            }
        }
    }

    private fun extractDevice(intent: Intent): BluetoothDevice? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    private fun bondedName(context: Context, address: String): String =
        runCatching {
            BluetoothUtils.getBondedDevices(context).find { it.address == address }?.name
        }.getOrNull() ?: address

    companion object {
        private val TAG = LogUtils.tag("Receiver")
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
