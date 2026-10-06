package com.kglabs28.btradiusdetector.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.kglabs28.btradiusdetector.data.BleRssiRepository
import com.kglabs28.btradiusdetector.data.local.AlertSettingsEntity
import com.kglabs28.btradiusdetector.data.local.AppDatabase
import com.kglabs28.btradiusdetector.service.StickyAlertService
import com.kglabs28.btradiusdetector.utils.Constants
import com.kglabs28.btradiusdetector.utils.NotificationUtils
import com.kglabs28.btradiusdetector.utils.Strings

class DisconnectCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val address = inputData.getString(KEY_ADDRESS) ?: return Result.failure()

        val dao = AppDatabase.getInstance(applicationContext).alertSettingsDao()
        // No row yet (user never opened alert settings) means all-true defaults —
        // a fresh device notifies rather than silently doing nothing.
        val settings = dao.getByAddress(address) ?: AlertSettingsEntity(address = address)
        val notificationId = NotificationUtils.disconnectNotificationId(address)

        // Alerts disabled after the fact, or the drop was a blip: retract the
        // immediate post so no stale alert lingers.
        if (!settings.monitoringEnabled || !settings.notifyOnDisconnect) {
            Log.d(TAG, "alerts now disabled, retracting: $address")
            NotificationUtils.cancelSafely(applicationContext, notificationId)
            return Result.success()
        }

        val repo = BleRssiRepository(applicationContext)
        if (repo.isConnected(address)) {
            Log.d(TAG, "device reconnected during verify window, retracting: $address")
            NotificationUtils.cancelSafely(applicationContext, notificationId)
            return Result.success()
        }

        // Still down: ensure the alert is showing (covers the case where the
        // immediate post never ran). Same ID, so this only refreshes.
        val deviceName = repo.getBondedDevices().find { it.address == address }?.name ?: address

        if (settings.keepNotifyingOnDisconnect) {
            StickyAlertService.start(applicationContext, address, deviceName)
        } else {
            val notification = NotificationUtils.buildDisconnectNotification(
                applicationContext,
                address,
                Strings.disconnectTitle(deviceName),
                NotificationUtils.disconnectBody(repo, settings, address),
                settings.soundEnabled,
                settings.vibrationEnabled
            )
            NotificationUtils.notifySafely(applicationContext, notificationId, notification)
            Log.d(TAG, "disconnect verified still down, alert ensured: $address")
        }

        return Result.success()
    }

    companion object {
        private const val TAG = "DisconnectCheckWorker"
        private const val KEY_ADDRESS = "address"
        fun buildInput(address: String): Data = Data.Builder().putString(KEY_ADDRESS, address).build()
    }
}