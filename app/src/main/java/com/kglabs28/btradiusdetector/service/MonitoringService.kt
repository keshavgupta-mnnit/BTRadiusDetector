package com.kglabs28.btradiusdetector.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kglabs28.btradiusdetector.R
import com.kglabs28.btradiusdetector.data.local.UserPreferencesRepository
import com.kglabs28.btradiusdetector.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MonitoringService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var preferencesRepository: UserPreferencesRepository
    private var monitoredAddresses = emptySet<String>()

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            device?.let {
                if (monitoredAddresses.contains(it.address)) {
                    when (action) {
                        BluetoothDevice.ACTION_ACL_CONNECTED -> {
                            showTransitionNotification(it, true)
                        }
                        BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                            showTransitionNotification(it, false)
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = UserPreferencesRepository(applicationContext)
        createNotificationChannel()

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(bluetoothReceiver, filter)

        serviceScope.launch {
            preferencesRepository.userPreferencesFlow.collect { prefs ->
                monitoredAddresses = prefs.monitoredDevices
                updateForegroundNotification()
            }
        }
    }

    private fun updateForegroundNotification() {
        val notification = createForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                Constants.FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(Constants.FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    private fun createForegroundNotification(): Notification {
        val deviceNames = getFormattedDeviceNames()
        val contentText = if (monitoredAddresses.isEmpty()) {
            "No devices being monitored"
        } else {
            "Monitoring: $deviceNames"
        }

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("BTRadius Monitoring")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun getFormattedDeviceNames(): String {
        if (monitoredAddresses.isEmpty()) return "None"
        val bluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter ?: return "Unknown"
        return monitoredAddresses.joinToString(", ") { address ->
            try {
                val device = bluetoothAdapter.getRemoteDevice(address)
                getDeviceName(device)
            } catch (_: Exception) {
                address
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceName(device: BluetoothDevice): String {
        return try {
            device.name ?: device.address
        } catch (e: SecurityException) {
            device.address
        }
    }

    private fun showTransitionNotification(device: BluetoothDevice, connected: Boolean) {
        val name = getDeviceName(device)
        val message = if (connected) "$name reconnected." else "Lost connection to $name"

        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Device Update")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Constants.TRANSITION_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateForegroundNotification()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
        serviceScope.cancel()
    }
}
