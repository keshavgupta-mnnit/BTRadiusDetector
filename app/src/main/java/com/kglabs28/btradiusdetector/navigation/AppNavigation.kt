package com.kglabs28.btradiusdetector.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.kglabs28.btradiusdetector.ui.screens.alertdetails.DeviceAlertDetailsScreen
import com.kglabs28.btradiusdetector.ui.screens.onboarding.OnboardingScreen
import com.kglabs28.btradiusdetector.ui.screens.settings.SettingsScreen
import com.kglabs28.btradiusdetector.ui.screens.tracking.TrackingScreen
import com.kglabs28.btradiusdetector.ui.screens.home.HomeScreen

/**
 * Navigation carries only primitives (device addresses) between destinations.
 * Every screen creates its own ViewModel internally — no ViewModel is ever
 * passed from one screen to the next.
 */
@Composable
fun AppNavigation(
    showOnboarding: Boolean,
    deepLinkDeviceId: String?,
    onDeepLinkConsumed: () -> Unit,
    onOnboardingComplete: () -> Unit
) {
    val initialRoute = if (showOnboarding) NavRoute.Onboarding else NavRoute.Scan
    val backStack = rememberNavBackStack(initialRoute)

    androidx.compose.runtime.LaunchedEffect(deepLinkDeviceId) {
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
                        onDeviceClick = { deviceId -> backStack.add(NavRoute.AlertDetails(deviceId)) }
                    )
                }

                is NavRoute.AlertDetails -> NavEntry(key) {
                    DeviceAlertDetailsScreen(
                        deviceId = key.deviceId,
                        onBack = { backStack.removeLastOrNull() }
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
