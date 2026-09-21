package com.kglabs28.btradiusdetector.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.kglabs28.btradiusdetector.utils.Constants
import com.kglabs28.btradiusdetector.utils.NotificationUtils
import com.kglabs28.btradiusdetector.utils.Strings

class StickyAlertService : Service() {

    private var timer: CountDownTimer? = null
    private var deviceAddress: String? = null
    private var deviceName: String? = null
    private var dismissReceiver: BroadcastReceiver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val address = intent?.getStringExtra(EXTRA_ADDRESS)
        val name = intent?.getStringExtra(EXTRA_NAME)
        if (address == null || name == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        deviceAddress = address
        deviceName = name
        activeAddress = address

        val notification = NotificationUtils.buildStickyAlertNotification(
            this,
            Strings.disconnectTitle(name),
            Strings.disconnectBody
        )
        startForeground(Constants.NOTIF_ID_STICKY, notification)

        registerDismissReceiver()
        startRepeatingTimer()

        return START_NOT_STICKY
    }

    private fun startRepeatingTimer() {
        timer?.cancel()
        timer = object : CountDownTimer(Long.MAX_VALUE, Constants.STICKY_REPEAT_INTERVAL_MS) {
            override fun onTick(millisUntilFinished: Long) {
                val name = deviceName ?: return
                val notification = NotificationUtils.buildStickyAlertNotification(
                    this@StickyAlertService,
                    Strings.disconnectTitle(name),
                    Strings.disconnectBody
                )
                NotificationUtils.notifySafely(this@StickyAlertService, Constants.NOTIF_ID_STICKY, notification)
            }
            override fun onFinish() {}
        }.start()
    }

    private fun registerDismissReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                stopSelf()
            }
        }
        dismissReceiver = receiver
        val filter = IntentFilter(ACTION_DISMISS)
        // ContextCompat.registerReceiver handles the exported/not-exported flag
        // uniformly across API levels, instead of branching manually.
        ContextCompat.registerReceiver(
            this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onDestroy() {
        timer?.cancel()
        dismissReceiver?.let { runCatching { unregisterReceiver(it) } }
        activeAddress = null
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.cancel(Constants.NOTIF_ID_STICKY)
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_ADDRESS = "extra_address"
        private const val EXTRA_NAME = "extra_name"
        const val ACTION_DISMISS = "com.kglabs28.btradiusdetector.ACTION_DISMISS_STICKY_ALERT"

        @Volatile
        private var activeAddress: String? = null

        fun isActiveFor(address: String): Boolean = activeAddress == address

        fun start(context: Context, address: String, deviceName: String) {
            val intent = Intent(context, StickyAlertService::class.java).apply {
                putExtra(EXTRA_ADDRESS, address)
                putExtra(EXTRA_NAME, deviceName)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            // Explicit package targeting is required: a RECEIVER_NOT_EXPORTED
            // receiver won't accept a plain implicit broadcast, even from
            // within the same app.
            val intent = Intent(ACTION_DISMISS).setPackage(context.packageName)
            context.sendBroadcast(intent)
        }
    }
}