package com.kglabs28.btradiusdetector.domain.usecase

import com.kglabs28.btradiusdetector.BTRadiusDetectorApp
import com.kglabs28.btradiusdetector.data.AlertSettingsRepository
import com.kglabs28.btradiusdetector.data.DisconnectIntentStore
import com.kglabs28.btradiusdetector.data.local.AppDatabase
import com.kglabs28.btradiusdetector.utils.BluetoothUtils
import com.kglabs28.btradiusdetector.utils.Constants
import com.kglabs28.btradiusdetector.utils.LogUtils
import com.kglabs28.btradiusdetector.utils.NotificationUtils
import com.kglabs28.btradiusdetector.utils.Strings

/**
 * The one place that decides and posts alerts: broadcast arrives, DB says
 * whether this device registered for that exact event, and only then we
 * notify. Used by the receiver (immediate) and app start (reconcile).
 * Strict opt-in throughout: no stored row means stay silent.
 *
 * Bluetooth reads go straight to [BluetoothUtils]; only the flags table has
 * a repository ([AlertSettingsRepository]).
 */
class EnsureAlertUseCase(
    private val settingsRepository: AlertSettingsRepository,
    private val intentStore: DisconnectIntentStore
) {

    private val appContext get() = BTRadiusDetectorApp.appContext

    /** Posts the disconnect alert when warranted. Returns true when posted. */
    suspend fun ensureDisconnect(address: String): Boolean {
        val settings = settingsRepository.getByAddress(address)
        val name = bondedName(address)
        LogUtils.d(TAG, "DB values existed - $name - $settings")
        if (settings == null) {
            intentStore.clear(address)
            return false
        }
        if (!settings.monitoringEnabled || !settings.notifyOnDisconnect) {
            LogUtils.d(TAG, "$name not registered for Disconnect Notification")
            LogUtils.d(TAG, "Hence Avoiding Notification for $name")
            return false
        }
        if (BluetoothUtils.isConnected(appContext, address)) {
            LogUtils.d(TAG, "Hence Avoiding Notification for $name")
            return false
        }
        if (!claimPostSlot("dis|$address")) {
            LogUtils.d(TAG, "Hence Avoiding Notification for $name")
            return false
        }
        LogUtils.d(TAG, "$name registered for Disconnect Notification")
        NotificationUtils.notifySafely(
            appContext,
            NotificationUtils.disconnectNotificationId(address),
            NotificationUtils.buildDisconnectNotification(
                appContext,
                address,
                Strings.disconnectTitle(name),
                NotificationUtils.disconnectBody(appContext, settings, address),
                settings.soundEnabled,
                settings.vibrationEnabled
            )
        )
        intentStore.clear(address)
        LogUtils.d(TAG, "Hence Triggering Notification for $name")
        return true
    }

    /** Posts the reconnect alert when warranted. Returns true when posted. */
    suspend fun ensureConnect(address: String): Boolean {
        val settings = settingsRepository.getByAddress(address)
        val name = bondedName(address)
        LogUtils.d(TAG, "DB values existed - $name - $settings")
        if (settings == null) return false
        if (!settings.monitoringEnabled || !settings.notifyOnReconnect) {
            LogUtils.d(TAG, "$name not registered for Connect Notification")
            LogUtils.d(TAG, "Hence Avoiding Notification for $name")
            return false
        }
        if (!claimPostSlot("con|$address")) {
            LogUtils.d(TAG, "Hence Avoiding Notification for $name")
            return false
        }
        LogUtils.d(TAG, "$name registered for Connect Notification")
        val battery = BluetoothUtils.getLastKnownBattery(appContext, address)
        NotificationUtils.notifySafely(
            appContext,
            Constants.NOTIF_ID_RECONNECT_BASE + address.hashCode(),
            NotificationUtils.buildReconnectNotification(
                appContext,
                address,
                Strings.reconnectTitle(name),
                battery?.let { Strings.reconnectBodyWithBattery(it) } ?: Strings.reconnectBody,
                settings.soundEnabled,
                settings.vibrationEnabled
            )
        )
        LogUtils.d(TAG, "Hence Triggering Notification for $name")
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
            runCatching { ensureDisconnect(address) }
        }
    }

    private fun bondedName(address: String): String =
        runCatching {
            BluetoothUtils.getBondedDevices(appContext).find { it.address == address }?.name
        }.getOrNull() ?: address

    /**
     * One physical flap emits several broadcasts (ACL + A2DP + headset).
     * First claim inside the window posts; the rest are duplicates.
     */
    private fun claimPostSlot(key: String): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        synchronized(recentPosts) {
            val last = recentPosts[key] ?: 0L
            if (now - last < Constants.ALERT_DEDUP_MS) return false
            recentPosts[key] = now
            return true
        }
    }

    companion object {
        private val TAG = LogUtils.tag("EnsureAlert")

        // Process-scoped by design: a fresh process has no recent posts.
        private val recentPosts = mutableMapOf<String, Long>()

        fun create(): EnsureAlertUseCase {
            val app = BTRadiusDetectorApp.appContext
            val settings = AlertSettingsRepository(
                AppDatabase.getInstance(app).alertSettingsDao()
            )
            return EnsureAlertUseCase(settings, DisconnectIntentStore(app))
        }
    }
}
