package com.kglabs28.btradiusdetector.domain.usecase

import android.content.Context
import android.util.Log
import com.kglabs28.btradiusdetector.data.AlertSettingsRepository
import com.kglabs28.btradiusdetector.data.BleRssiRepository
import com.kglabs28.btradiusdetector.data.DisconnectIntentStore
import com.kglabs28.btradiusdetector.service.StickyAlertService
import com.kglabs28.btradiusdetector.utils.Constants
import com.kglabs28.btradiusdetector.utils.NotificationUtils
import com.kglabs28.btradiusdetector.utils.Strings

/**
 * Single home for "should we alert about this drop, and post it".
 * Called from the manifest receiver, the monitoring service and the verify
 * worker — all three share decisions and the stable notification ID, so
 * duplicate paths collapse into one row instead of double posting.
 */
class EnsureDisconnectAlertUseCase(
    private val appContext: Context,
    private val settingsRepository: AlertSettingsRepository,
    private val bleRepository: BleRssiRepository,
    private val intentStore: DisconnectIntentStore
) {

    /** Posts the disconnect alert when warranted. Returns true when posted. */
    suspend fun ensure(address: String): Boolean {
        // Strict opt-in: no stored row means never registered — stay silent.
        // (Toggling monitoring on creates an all-true row; see entity defaults.)
        val settings = settingsRepository.getByAddress(address) ?: run {
            intentStore.clear(address)
            return false
        }
        if (!settings.monitoringEnabled || !settings.notifyOnDisconnect) {
            Log.d(TAG, "not registered for disconnect, skipping: $address")
            return false
        }
        if (bleRepository.isConnected(address)) {
            Log.d(TAG, "link alive, no alert: $address")
            return false
        }
        val name = bondedName(address)
        if (settings.keepNotifyingOnDisconnect) {
            StickyAlertService.start(appContext, address, name)
        } else {
            NotificationUtils.notifySafely(
                appContext,
                NotificationUtils.disconnectNotificationId(address),
                NotificationUtils.buildDisconnectNotification(
                    appContext,
                    address,
                    Strings.disconnectTitle(name),
                    NotificationUtils.disconnectBody(bleRepository, settings, address),
                    settings.soundEnabled,
                    settings.vibrationEnabled
                )
            )
        }
        intentStore.clear(address)
        Log.d(TAG, "disconnect alert posted: $address")
        return true
    }

    /**
     * Reconciles drops recorded while no process survived to post them
     * (the "appears only after app opens" gap). Fresh, still-down and still
     * wanted drops post now; everything else is dropped silently.
     */
    suspend fun reconcilePending() {
        val pending = intentStore.freshPending(Constants.PENDING_ALERT_STALE_MS)
        intentStore.dropStale(Constants.PENDING_ALERT_STALE_MS)
        pending.keys.forEach { address ->
            runCatching { ensure(address) }
        }
    }

    private fun bondedName(address: String): String =
        runCatching { bleRepository.getBondedDevices().find { it.address == address }?.name }.getOrNull()
            ?: address

    companion object {
        private const val TAG = "EnsureDisconnectAlert"

        fun create(context: Context): EnsureDisconnectAlertUseCase {
            val app = context.applicationContext
            val settings = AlertSettingsRepository(
                com.kglabs28.btradiusdetector.data.local.AppDatabase.getInstance(app).alertSettingsDao()
            )
            return EnsureDisconnectAlertUseCase(app, settings, BleRssiRepository(app), DisconnectIntentStore(app))
        }
    }
}
