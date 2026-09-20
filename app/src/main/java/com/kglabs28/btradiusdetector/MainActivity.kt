package com.kglabs28.btradiusdetector

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.kglabs28.btradiusdetector.data.BleRssiRepository
import com.kglabs28.btradiusdetector.data.CompassRepository
import com.kglabs28.btradiusdetector.data.local.UserPreferencesRepository
import com.kglabs28.btradiusdetector.navigation.NavRoute
import com.kglabs28.btradiusdetector.ui.MainViewModel
import com.kglabs28.btradiusdetector.ui.screens.TrackingScreen
import com.kglabs28.btradiusdetector.ui.screens.OnboardingScreen
import com.kglabs28.btradiusdetector.ui.screens.ScanScreen
import com.kglabs28.btradiusdetector.ui.screens.SettingsScreen
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kglabs28.btradiusdetector.service.MonitoringService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val preferencesRepository = UserPreferencesRepository(applicationContext)
        val bleRepository = BleRssiRepository(applicationContext)
        val compassRepository = CompassRepository(applicationContext)
        
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(preferencesRepository, bleRepository, compassRepository) as T
            }
        }

        startMonitoringService()

        setContent {
            val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
            val preferences by viewModel.userPreferences.collectAsState()

            BTRadiusDetectorTheme {
                if (preferences != null) {
                    key(preferences?.showOnboarding) {
                        BTRadiusDetectorApp(
                            viewModel = viewModel,
                            showOnboarding = preferences?.showOnboarding ?: true
                        )
                    }
                }
            }
        }
    }

    private fun startMonitoringService() {
        if (!hasRequiredPermissions()) return

        val intent = Intent(this, MonitoringService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(android.Manifest.permission.BLUETOOTH_SCAN)
        } else {
            permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions.all {
            androidx.core.content.ContextCompat.checkSelfPermission(this, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}

@Composable
fun BTRadiusDetectorApp(
    viewModel: MainViewModel,
    showOnboarding: Boolean
) {
    val initialRoute = if (showOnboarding) NavRoute.Onboarding else NavRoute.Scan
    val backStack = rememberNavBackStack(initialRoute)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = { key ->
            when (key) {
                is NavRoute.Onboarding -> NavEntry(key) { OnboardingScreen(viewModel = viewModel) }
                is NavRoute.Scan -> NavEntry(key) { 
                    ScanScreen(
                        viewModel = viewModel,
                        onDeviceSelected = { deviceId ->
                            backStack.add(NavRoute.Details(deviceId))
                        },
                        onSettingsClick = {
                            backStack.add(NavRoute.Settings)
                        }
                    ) 
                }
                is NavRoute.Settings -> NavEntry(key) { 
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { backStack.removeLastOrNull() }
                    ) 
                }
                is NavRoute.Details -> NavEntry(key) { 
                    TrackingScreen(
                        deviceId = key.deviceId, 
                        viewModel = viewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onSettingsClick = {
                            backStack.add(NavRoute.Settings)
                        }
                    ) 
                }
                else -> NavEntry(key as NavKey) { /* Handle unknown */ }
            }
        }
    )
}
