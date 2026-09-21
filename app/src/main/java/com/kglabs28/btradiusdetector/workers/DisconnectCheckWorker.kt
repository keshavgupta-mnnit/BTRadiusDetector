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

class DisconnectCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val address = inputData.getString(KEY_ADDRESS) ?: return Result.failure()

        val dao = AppDatabase.getInstance(applicationContext).alertSettingsDao()
        val settings = dao.getByAddress(address) ?: return Result.success()
        if (!settings.notifyOnDisconnect) return Result.success()

        val repo = BleRssiRepository(applicationContext)
        val stillDisconnected = !repo.isConnected(address)
        if (!stillDisconnected) return Result.success() // reconnected during the debounce window

        val name = settings.address // fallback if name lookup unavailable at this point
        val deviceName = repo.getBondedDevices().find { it.address == address }?.name ?: name

        if (settings.keepNotifyingOnDisconnect) {
            StickyAlertService.start(applicationContext, address, deviceName)
        } else {
            val notification = NotificationUtils.buildOneTimeAlertNotification(
                applicationContext,
                Strings.disconnectTitle(deviceName),
                Strings.disconnectBody,
                settings.soundEnabled
            )
            NotificationUtils.notifySafely(applicationContext,Constants.NOTIF_ID_DISCONNECT_BASE + address.hashCode(), notification)
        }

        return Result.success()
    }

    companion object {
        private const val KEY_ADDRESS = "address"
        fun buildInput(address: String): Data = Data.Builder().putString(KEY_ADDRESS, address).build()
    }
}