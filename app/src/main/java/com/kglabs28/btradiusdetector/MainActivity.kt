package com.kglabs28.btradiusdetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.kglabs28.btradiusdetector.navigation.AppNavigation
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.utils.AppUtils
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val showOnboarding by AppUtils.observeShowOnboarding(applicationContext)
                .collectAsState(initial = true)

            BTRadiusDetectorTheme {
                AppNavigation(
                    showOnboarding = false,
                    onOnboardingComplete = {
                        lifecycleScope.launch {
                            AppUtils.setShowOnboarding(applicationContext, false)
                        }
                    }
                )
            }
        }
    }
}
