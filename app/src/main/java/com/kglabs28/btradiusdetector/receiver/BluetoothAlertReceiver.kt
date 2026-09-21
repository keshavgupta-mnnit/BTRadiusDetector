package com.kglabs28.btradiusdetector.receiver

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kglabs28.btradiusdetector.utils.Constants
import com.kglabs28.btradiusdetector.workers.ConnectEventWorker
import com.kglabs28.btradiusdetector.workers.DisconnectCheckWorker
import java.util.concurrent.TimeUnit

class BluetoothAlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
        val address = device.address
        val workManager = WorkManager.getInstance(context)

        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                // A reconnect cancels any pending disconnect check for this
                // device — the earlier disconnect was a blip, not real.
                workManager.cancelUniqueWork(disconnectWorkName(address))

                val request = OneTimeWorkRequestBuilder<ConnectEventWorker>()
                    .setInputData(ConnectEventWorker.buildInput(address))
                    .build()
                workManager.enqueueUniqueWork(
                    connectWorkName(address),
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
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
        }
    }

    companion object {
        private fun disconnectWorkName(address: String) = "disconnect_check_$address"
        private fun connectWorkName(address: String) = "connect_event_$address"
    }
}