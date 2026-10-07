package com.kglabs28.btradiusdetector

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.kglabs28.btradiusdetector.navigation.AppNavigation
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.utils.AppUtils
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var deepLinkDeviceId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Stale shade rows from previous installs (old text/design) die here —
        // opening the app means their job is done.
        runCatching { androidx.core.app.NotificationManagerCompat.from(this).cancelAll() }
        deepLinkDeviceId = intent?.getStringExtra(EXTRA_DEVICE_ADDRESS)

        setContent {
            val showOnboarding by AppUtils.observeShowOnboarding(applicationContext)
                .collectAsState(initial = true)

            BTRadiusDetectorTheme {
                AppNavigation(
                    showOnboarding = false,
                    deepLinkDeviceId = deepLinkDeviceId,
                    onDeepLinkConsumed = { deepLinkDeviceId = null },
                    onOnboardingComplete = {
                        lifecycleScope.launch {
                            AppUtils.setShowOnboarding(applicationContext, false)
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Tapping a notification while the app is alive pushes tracking on top.
        deepLinkDeviceId = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
    }

    companion object {
        const val ACTION_TRACK_DEVICE =
            "com.kglabs28.btradiusdetector.ACTION_TRACK_DEVICE"
        const val EXTRA_DEVICE_ADDRESS = "extra_device_address"
    }
}
