package com.kglabs28.btradiusdetector.navigation

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.kglabs28.btradiusdetector.ui.components.BatteryGateContent
import com.kglabs28.btradiusdetector.ui.components.PermissionRationaleDialog
import com.kglabs28.btradiusdetector.ui.components.PermissionRequestContent
import com.kglabs28.btradiusdetector.ui.screens.alertdetails.DeviceAlertDetailsScreen
import com.kglabs28.btradiusdetector.ui.screens.history.HistoryScreen
import com.kglabs28.btradiusdetector.ui.screens.onboarding.OnboardingScreen
import com.kglabs28.btradiusdetector.ui.screens.settings.SettingsScreen
import com.kglabs28.btradiusdetector.ui.screens.tracking.TrackingScreen
import com.kglabs28.btradiusdetector.ui.screens.home.HomeScreen
import com.kglabs28.btradiusdetector.utils.AppUtils

/**
 * Navigation carries only primitives (device addresses) between destinations.
 * Every screen creates its own ViewModel internally — no ViewModel is ever
 * passed from one screen to the next.
 *
 * Permissions are a hard gate: nothing past this point renders until they
 * are granted, so no screen needs its own fallback for missing permissions.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppNavigation(
    showOnboarding: Boolean,
    deepLinkDeviceId: String?,
    onDeepLinkConsumed: () -> Unit,
    onOnboardingComplete: () -> Unit
) {
    val permissionsToRequest = remember {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN)
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        // Classic discovery RSSI is location-derived on every API level.
        list.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list
    }
    val permissionState = rememberMultiplePermissionsState(permissionsToRequest)
    var showRationaleDialog by remember { mutableStateOf(false) }

    // Battery whitelist status refreshes on every resume (back from system settings).
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeTick by remember { mutableStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val batteryUnrestricted = remember(resumeTick) { AppUtils.isBatteryOptimizationIgnored(context) }
    var batteryGateDismissed by remember { mutableStateOf(false) }

    if (!permissionState.allPermissionsGranted) {
        if (showRationaleDialog) {
            PermissionRationaleDialog(
                onConfirm = { showRationaleDialog = false; permissionState.launchMultiplePermissionRequest() },
                onDismiss = { showRationaleDialog = false }
            )
        }
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize()) {
                PermissionRequestContent(
                    onRequestPermissions = {
                        if (permissionState.shouldShowRationale) showRationaleDialog = true
                        else permissionState.launchMultiplePermissionRequest()
                    },
                    batteryUnrestricted = batteryUnrestricted,
                    onBatteryClick = { activity?.let { AppUtils.requestIgnoreBatteryOptimizations(it) } }
                )
            }
        }
        return
    }

    // Runtime permissions pass, but without the whitelist Doze can still
    // hold alerts until the app opens — one explicit gate for it.
    if (!batteryUnrestricted && !batteryGateDismissed) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BatteryGateContent(
                onAllow = { activity?.let { AppUtils.requestIgnoreBatteryOptimizations(it) } },
                onSkip = { batteryGateDismissed = true }
            )
        }
        return
    }

    val initialRoute = if (showOnboarding) NavRoute.Onboarding else NavRoute.Scan
    val backStack = rememberNavBackStack(initialRoute)

    LaunchedEffect(deepLinkDeviceId) {
        if (deepLinkDeviceId != null) {
            backStack.add(NavRoute.Details(deepLinkDeviceId))
            onDeepLinkConsumed()
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = { key ->
            when (key) {
                is NavRoute.Onboarding -> NavEntry(key) {
                    OnboardingScreen(onComplete = onOnboardingComplete)
                }

                is NavRoute.Scan -> NavEntry(key) {
                    HomeScreen(
                        onDeviceSelected = { deviceId -> backStack.add(NavRoute.Details(deviceId)) },
                        onSettingsClick = { backStack.add(NavRoute.Settings) }
                    )
                }

                is NavRoute.Settings -> NavEntry(key) {
                    SettingsScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onDeviceClick = { deviceId -> backStack.add(NavRoute.AlertDetails(deviceId)) },
                        onHistoryClick = { backStack.add(NavRoute.History) }
                    )
                }

                is NavRoute.History -> NavEntry(key) {
                    HistoryScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                is NavRoute.AlertDetails -> NavEntry(key) {
                    DeviceAlertDetailsScreen(
                        deviceId = key.deviceId,
                        onBack = { backStack.removeLastOrNull() },
                        onHistoryClick = { backStack.add(NavRoute.History) }
                    )
                }

                is NavRoute.Details -> NavEntry(key) {
                    TrackingScreen(
                        deviceId = key.deviceId,
                        onBack = { backStack.removeLastOrNull() },
                        onSettingsClick = { backStack.add(NavRoute.AlertDetails(key.deviceId)) }
                    )
                }

                else -> NavEntry(key as NavKey) { /* unknown route */ }
            }
        }
    )
}
