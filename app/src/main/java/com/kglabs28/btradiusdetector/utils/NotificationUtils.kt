package com.kglabs28.btradiusdetector.utils

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kglabs28.btradiusdetector.MainActivity
import com.kglabs28.btradiusdetector.R
import com.kglabs28.btradiusdetector.data.BleRssiRepository
import com.kglabs28.btradiusdetector.data.local.AlertSettingsEntity
import com.kglabs28.btradiusdetector.ui.theme.NotificationAccentDisconnect
import com.kglabs28.btradiusdetector.ui.theme.NotificationAccentReconnect

object NotificationUtils {

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val silent = NotificationChannel(
            Constants.CHANNEL_ID_SILENT,
            context.getString(R.string.notification_channel_silent_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setSound(null, null)
            enableVibration(false)
        }

        val sound = NotificationChannel(
            Constants.CHANNEL_ID_SOUND,
            context.getString(R.string.notification_channel_sound_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
        }

        val vibrate = NotificationChannel(
            Constants.CHANNEL_ID_VIBRATE,
            context.getString(R.string.notification_channel_vibrate_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(null, null)
            enableVibration(true)
        }

        val sticky = NotificationChannel(
            Constants.CHANNEL_ID_STICKY,
            context.getString(R.string.notification_channel_sticky_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
        }

        manager.createNotificationChannels(listOf(silent, sound, vibrate, sticky))
    }

    /**
     * Shared base matching the app's notification design:
     * - Custom collapsed + expanded views: accent rounded icon, bold title, body.
     * - Stock title/body/intent/timestamp stay set so watches and stripped
     *   views still render something sane.
     * - Accent color: Red for disconnect, Mint Green for reconnect & monitoring.
     * - Tap action: opens the device tracking screen.
     */
    private fun baseAlertBuilder(
        context: Context,
        deviceAddress: String?,
        channelId: String,
        title: String,
        body: String,
        accent: Color,
        isDisconnect: Boolean
    ): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_bt)
            .setContentTitle(title)
            .setContentIntent(trackingPendingIntent(context, deviceAddress))
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setColor(accent.toArgb())

        if (body.isNotBlank()) {
            builder.setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        return builder
    }

    /** Red disconnect alert: "Lost connection to X" / "Device may be out of range." */
    fun buildDisconnectNotification(
        context: Context,
        deviceAddress: String,
        title: String,
        body: String,
        useSound: Boolean,
        useVibration: Boolean
    ): Notification {
        val channelId = when {
            useSound -> Constants.CHANNEL_ID_SOUND
            useVibration -> Constants.CHANNEL_ID_VIBRATE
            else -> Constants.CHANNEL_ID_SILENT
        }
        return baseAlertBuilder(context, deviceAddress, channelId, title, body, NotificationAccentDisconnect, isDisconnect = true)
            .setAutoCancel(true)
            .setPriority(if (useSound || useVibration) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /** Green reconnect alert: "X reconnected." */
    fun buildReconnectNotification(
        context: Context,
        deviceAddress: String,
        title: String,
        body: String,
        useSound: Boolean,
        useVibration: Boolean
    ): Notification {
        val channelId = when {
            useSound -> Constants.CHANNEL_ID_SOUND
            useVibration -> Constants.CHANNEL_ID_VIBRATE
            else -> Constants.CHANNEL_ID_SILENT
        }
        return baseAlertBuilder(context, deviceAddress, channelId, title, body, NotificationAccentReconnect, isDisconnect = false)
            .setAutoCancel(true)
            .setPriority(if (useSound || useVibration) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /** Ongoing persistent monitoring / disconnect alert */
    fun buildStickyAlertNotification(
        context: Context,
        deviceAddress: String,
        title: String,
        body: String
    ): Notification {
        return baseAlertBuilder(
            context,
            deviceAddress,
            Constants.CHANNEL_ID_STICKY,
            title,
            body,
            NotificationAccentDisconnect,
            isDisconnect = true
        )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    fun trackingPendingIntent(context: Context, deviceAddress: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_TRACK_DEVICE
            if (deviceAddress != null) putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, deviceAddress)
        }
        return PendingIntent.getActivity(
            context,
            deviceAddress?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Ongoing "Monitoring" notification (green, persistent): shows the live
     * monitored device with battery, or the idle state. Tapping opens that
     * device's tracking screen, or the app home when nothing is live.
     */
    fun buildMonitoringNotification(
        context: Context,
        deviceAddress: String?,
        title: String,
        body: String
    ): android.app.Notification {
        return baseAlertBuilder(
            context,
            deviceAddress,
            Constants.CHANNEL_ID_STICKY,
            title,
            body,
            NotificationAccentReconnect,
            isDisconnect = false
        )
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun disconnectNotificationId(address: String): Int =
        Constants.NOTIF_ID_DISCONNECT_BASE + address.hashCode()

    fun reconnectNotificationId(address: String): Int =
        Constants.NOTIF_ID_RECONNECT_BASE + address.hashCode()

    fun cancelSafely(context: Context, notificationId: Int) {
        runCatching { NotificationManagerCompat.from(context).cancel(notificationId) }
    }

    fun disconnectBody(
        repo: BleRssiRepository,
        settings: AlertSettingsEntity,
        address: String
    ): String {
        var body = repo.getLastKnownBattery(address)?.let { Strings.disconnectBodyWithBattery(it) }
            ?: Strings.disconnectBody
        if (settings.lastBestRssi > Constants.RSSI_FLOOR) {
            body += " " + Strings.lastSeenLabel(
                settings.lastBestHeading.toInt(),
                AppUtils.getCardinalDirection(settings.lastBestHeading)
            )
        }
        return body
    }

    fun notifySafely(context: Context, notificationId: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
        }
    }
}