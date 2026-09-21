package com.kglabs28.btradiusdetector.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.kglabs28.btradiusdetector.data.BleRssiRepository
import com.kglabs28.btradiusdetector.data.local.AppDatabase
import com.kglabs28.btradiusdetector.service.StickyAlertService
import com.kglabs28.btradiusdetector.utils.Constants
import com.kglabs28.btradiusdetector.utils.NotificationUtils
import com.kglabs28.btradiusdetector.utils.Strings

class ConnectEventWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val address = inputData.getString(KEY_ADDRESS) ?: return Result.failure()

        // A reconnect always stops any active sticky alert for this device,
        // regardless of notify-on-reconnect being on.
        if (StickyAlertService.isActiveFor(address)) {
            StickyAlertService.stop(applicationContext)
        }

        val dao = AppDatabase.getInstance(applicationContext).alertSettingsDao()
        val settings = dao.getByAddress(address) ?: return Result.success()
        if (!settings.notifyOnReconnect) return Result.success()

        val repo = BleRssiRepository(applicationContext)
        val deviceName = repo.getBondedDevices().find { it.address == address }?.name ?: address

        val notification = NotificationUtils.buildOneTimeAlertNotification(
            applicationContext,
            Strings.reconnectTitle(deviceName),
            Strings.reconnectBody,
            settings.soundEnabled
        )
        NotificationUtils.notifySafely(
            applicationContext,
            Constants.NOTIF_ID_RECONNECT_BASE + address.hashCode(),
            notification
        )

        return Result.success()
    }

    companion object {
        private const val KEY_ADDRESS = "address"
        fun buildInput(address: String): Data =
            Data.Builder().putString(KEY_ADDRESS, address).build()
    }
}