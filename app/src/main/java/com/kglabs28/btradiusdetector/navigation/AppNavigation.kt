package com.kglabs28.btradiusdetector.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.kglabs28.btradiusdetector.ui.MainViewModel
import com.kglabs28.btradiusdetector.ui.screens.OnboardingScreen
import com.kglabs28.btradiusdetector.ui.screens.SettingsScreen
import com.kglabs28.btradiusdetector.ui.screens.TrackingScreen
import com.kglabs28.btradiusdetector.ui.screens.home.HomeScreen

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    showOnboarding: Boolean,
    onOnboardingComplete: () -> Unit
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
                    SettingsScreen(viewModel = viewModel, onBack = { backStack.removeLastOrNull() })
                }

                is NavRoute.Details -> NavEntry(key) {
                    TrackingScreen(
                        deviceId = key.deviceId,
                        viewModel = viewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onSettingsClick = { backStack.add(NavRoute.Settings) }
                    )
                }

                else -> NavEntry(key as NavKey) { /* unknown route */ }
            }
        }
    )
}