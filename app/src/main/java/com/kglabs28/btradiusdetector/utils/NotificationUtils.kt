package com.kglabs28.btradiusdetector.utils

import android.Manifest
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
import com.kglabs28.btradiusdetector.ui.theme.NotificationAccentDisconnect
import com.kglabs28.btradiusdetector.ui.theme.NotificationAccentReconnect
import com.kglabs28.btradiusdetector.utils.AppUtils

object NotificationUtils {

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

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
     * Shared base matching the app's notification design: bold title, body,
     * timestamp, accent-tinted icon, tap opens the device's tracking screen.
     */
    private fun baseAlertBuilder(
        context: Context,
        deviceAddress: String,
        channelId: String,
        title: String,
        body: String,
        accent: Color
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(trackingPendingIntent(context, deviceAddress))
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setColor(accent.toArgb())
            .setLargeIcon(appIconBitmap(context))
    }

    private fun appIconBitmap(context: Context): android.graphics.Bitmap? {
        return runCatching {
            val drawable: android.graphics.drawable.Drawable =
                ContextCompat.getDrawable(context, R.drawable.ic_notification)
                    ?: return@runCatching null
            val width = drawable.intrinsicWidth.coerceAtLeast(1)
            val height = drawable.intrinsicHeight.coerceAtLeast(1)
            val bitmap = android.graphics.Bitmap.createBitmap(
                width, height, android.graphics.Bitmap.Config.ARGB_8888
            )
            drawable.setBounds(0, 0, width, height)
            drawable.draw(android.graphics.Canvas(bitmap))
            bitmap
        }.getOrNull()
    }

    /** Red disconnect alert: "Lost connection to X / Device may be out of range." */
    fun buildDisconnectNotification(
        context: Context,
        deviceAddress: String,
        title: String,
        body: String,
        useSound: Boolean,
        useVibration: Boolean
    ): android.app.Notification {
        val channelId = when {
            useSound -> Constants.CHANNEL_ID_SOUND
            useVibration -> Constants.CHANNEL_ID_VIBRATE
            else -> Constants.CHANNEL_ID_SILENT
        }
        return baseAlertBuilder(context, deviceAddress, channelId, title, body, NotificationAccentDisconnect)
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
    ): android.app.Notification {
        val channelId = when {
            useSound -> Constants.CHANNEL_ID_SOUND
            useVibration -> Constants.CHANNEL_ID_VIBRATE
            else -> Constants.CHANNEL_ID_SILENT
        }
        return baseAlertBuilder(context, deviceAddress, channelId, title, body, NotificationAccentReconnect)
            .setAutoCancel(true)
            .setPriority(if (useSound || useVibration) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun trackingPendingIntent(context: Context, deviceAddress: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_TRACK_DEVICE
            putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, deviceAddress)
        }
        return PendingIntent.getActivity(
            context,
            deviceAddress.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Stable ID so immediate posts, worker re-posts and retractions hit one row. */
    fun disconnectNotificationId(address: String): Int =
        Constants.NOTIF_ID_DISCONNECT_BASE + address.hashCode()

    fun cancelSafely(context: Context, notificationId: Int) {
        runCatching { NotificationManagerCompat.from(context).cancel(notificationId) }
    }

    /**
     * Disconnect body shared by the immediate post and the verifier pass:
     * base hint + battery when known + last best bearing when recorded.
     */
    fun disconnectBody(
        repo: com.kglabs28.btradiusdetector.data.BleRssiRepository,
        settings: com.kglabs28.btradiusdetector.data.local.AlertSettingsEntity,
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

    fun buildOneTimeAlertNotification(
        context: Context,
        title: String,
        body: String,
        useSound: Boolean
    ): android.app.Notification {
        val channelId = if (useSound) Constants.CHANNEL_ID_SOUND else Constants.CHANNEL_ID_SILENT
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification) // ensure this drawable exists; placeholder if not
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(if (useSound) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun buildStickyAlertNotification(
        context: Context,
        deviceAddress: String,
        title: String,
        body: String
    ): android.app.Notification {
        return baseAlertBuilder(
            context,
            deviceAddress,
            Constants.CHANNEL_ID_STICKY,
            title,
            body,
            NotificationAccentDisconnect
        )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    fun notifySafely(context: Context, notificationId: Int, notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check above and this call — safe to ignore.
        }
    }
}