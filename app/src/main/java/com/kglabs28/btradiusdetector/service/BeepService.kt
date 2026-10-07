package com.kglabs28.btradiusdetector.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.kglabs28.btradiusdetector.utils.Constants
import com.kglabs28.btradiusdetector.utils.LogUtils
import com.kglabs28.btradiusdetector.utils.NotificationUtils
import com.kglabs28.btradiusdetector.utils.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Continuous-beep service: loops the notification sound (and vibration
 * pulses, when enabled) until the user interacts — taps the alert (opens
 * tracking), hits Stop, or the device reconnects. Runs only while beeping;
 * every stop path below kills it, so it never lingers.
 */
class BeepService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: MediaPlayer? = null
    private var buzzJob: Job? = null
    private var deviceAddress: String? = null
    private var deviceName: String? = null
    private var vibrate: Boolean = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val address = intent?.getStringExtra(EXTRA_ADDRESS)
        val name = intent?.getStringExtra(EXTRA_NAME)
        if (address == null || name == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        // A second device takes over the single loop — one beep at a time.
        stopLoop()
        activeAddress = address
        deviceAddress = address
        deviceName = name
        vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)

        try {
            startForegroundWithType(
                NotificationUtils.buildBeepingNotification(
                    this,
                    address,
                    Strings.disconnectTitle(name),
                    Strings.disconnectBody,
                    vibrate
                )
            )
        } catch (e: SecurityException) {
            LogUtils.w(TAG, "foreground start denied, stopping", e)
            stopSelf()
            return START_NOT_STICKY
        }
        startLoop()
        LogUtils.d(TAG, "beeping started for $name")
        return START_STICKY
    }

    override fun onDestroy() {
        activeAddress = null
        stopLoop()
        scope.cancel()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swipe-away counts as interaction: stop the noise.
        stopSelf()
    }

    private fun startLoop() {
        val soundUri = android.media.RingtoneManager.getDefaultUri(
            android.media.RingtoneManager.TYPE_NOTIFICATION
        )
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            runCatching {
                setDataSource(this@BeepService, soundUri)
                isLooping = true
                prepare()
                start()
            }.onFailure {
                LogUtils.e(TAG, "beep loop failed to start", it)
                stopSelf()
            }
        }
        if (vibrate) {
            buzzJob = scope.launch {
                while (true) {
                    vibrateOnce()
                    delay(Constants.BEEP_VIBRATE_INTERVAL_MS)
                }
            }
        }
    }

    private fun vibrateOnce() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        Constants.BEEP_VIBRATE_DURATION_MS,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(Constants.BEEP_VIBRATE_DURATION_MS)
            }
        }
    }

    private fun stopLoop() {
        buzzJob?.cancel()
        buzzJob = null
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
    }

    private fun startForegroundWithType(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                Constants.NOTIF_ID_BEEPING,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(Constants.NOTIF_ID_BEEPING, notification)
        }
    }

    companion object {
        private val TAG = LogUtils.tag("Beep")

        private const val EXTRA_ADDRESS = "extra_address"
        private const val EXTRA_NAME = "extra_name"
        private const val EXTRA_VIBRATE = "extra_vibrate"

        @Volatile
        private var activeAddress: String? = null

        /** Returns false when the service refused to start (permissions). */
        fun start(context: Context, address: String, deviceName: String, vibrate: Boolean): Boolean {
            return runCatching {
                val intent = Intent(context, BeepService::class.java).apply {
                    putExtra(EXTRA_ADDRESS, address)
                    putExtra(EXTRA_NAME, deviceName)
                    putExtra(EXTRA_VIBRATE, vibrate)
                }
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
                LogUtils.d(TAG, "start requested for $deviceName")
                true
            }.onFailure {
                LogUtils.w(TAG, "beep service start denied", it)
            }.getOrDefault(false)
        }

        fun stop(context: Context) {
            activeAddress = null
            runCatching { context.stopService(Intent(context, BeepService::class.java)) }
            NotificationUtils.cancelSafely(context, Constants.NOTIF_ID_BEEPING)
            LogUtils.d(TAG, "stop requested")
        }

        /** Stops only when this device owns the loop — never a stranger's beep. */
        fun stopFor(context: Context, address: String) {
            if (activeAddress == address) stop(context)
        }
    }
}
