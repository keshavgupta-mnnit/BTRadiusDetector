package com.kglabs28.btradiusdetector.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.kglabs28.btradiusdetector.data.BleRssiRepository
import com.kglabs28.btradiusdetector.data.DisconnectIntentStore
import com.kglabs28.btradiusdetector.data.local.AppDatabase
import com.kglabs28.btradiusdetector.domain.usecase.EnsureDisconnectAlertUseCase
import com.kglabs28.btradiusdetector.utils.NotificationUtils

/**
 * Verify pass for disconnects: retracts blips and disabled alerts, ensures a
 * still-down device is showing its alert. Runs after the debounce window and
 * — crucially — runs even if the immediate post never happened, because
 * WorkManager persists it across process death.
 */
class DisconnectCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val address = inputData.getString(KEY_ADDRESS) ?: return Result.failure()
        val appContext = applicationContext
        val notificationId = NotificationUtils.disconnectNotificationId(address)
        val useCase = EnsureDisconnectAlertUseCase.create(appContext)

        val dao = AppDatabase.getInstance(appContext).alertSettingsDao()
        val settings = dao.getByAddress(address)
        val repo = BleRssiRepository(appContext)

        // Never registered, or unregistered since: retract and stay silent.
        if (settings == null || !settings.monitoringEnabled || !settings.notifyOnDisconnect) {
            Log.d(TAG, "not registered for disconnect, retracting: $address")
            NotificationUtils.cancelSafely(appContext, notificationId)
            DisconnectIntentStore(appContext).clear(address)
            return Result.success()
        }
        if (repo.isConnected(address)) {
            Log.d(TAG, "device reconnected during verify window, retracting: $address")
            NotificationUtils.cancelSafely(appContext, notificationId)
            DisconnectIntentStore(appContext).clear(address)
            return Result.success()
        }

        // Still down (or never posted): ensure the alert is showing.
        useCase.ensure(address)
        Log.d(TAG, "disconnect verified, alert ensured: $address")
        return Result.success()
    }

    companion object {
        private const val TAG = "DisconnectCheckWorker"
        private const val KEY_ADDRESS = "address"
        fun buildInput(address: String): Data = Data.Builder().putString(KEY_ADDRESS, address).build()
    }
}
