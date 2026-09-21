package com.kglabs28.btradiusdetector.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kglabs28.btradiusdetector.R

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

        val sticky = NotificationChannel(
            Constants.CHANNEL_ID_STICKY,
            context.getString(R.string.notification_channel_sticky_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
        }

        manager.createNotificationChannels(listOf(silent, sound, sticky))
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
        title: String,
        body: String
    ): android.app.Notification {
        return NotificationCompat.Builder(context, Constants.CHANNEL_ID_STICKY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
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