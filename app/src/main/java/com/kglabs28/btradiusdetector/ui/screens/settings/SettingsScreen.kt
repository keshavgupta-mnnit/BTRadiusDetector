package com.kglabs28.btradiusdetector.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kglabs28.btradiusdetector.ui.components.AboutSection
import com.kglabs28.btradiusdetector.ui.components.RangeAlertsSection
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.utils.AppUtils
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

/**
 * Range Alerts list. Only navigation primitives cross the boundary
 * ([onDeviceClick] carries just the address) — this screen never touches
 * another screen's ViewModel or repository.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onDeviceClick: (String) -> Unit,
    onHistoryClick: () -> Unit
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory())
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    // Re-check on every resume (e.g. back from the system whitelist screen).
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
    val notificationsEnabled = remember(resumeTick) { AppUtils.areNotificationsEnabled(context) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(Strings.settingsTitle, fontWeight = FontWeight.Bold, fontSize = Dimens.textTitleSmall)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = Strings.backDesc)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .padding(horizontal = Dimens.screenPaddingH.scaled())
                .verticalScroll(rememberScrollState())
        ) {
            RangeAlertsSection(
                devices = state.devices,
                alertEnabled = state.alertEnabled,
                onToggleAlert = viewModel::setAlertEnabled,
                onDeviceClick = onDeviceClick
            )
            AboutSection(
                batteryUnrestricted = batteryUnrestricted,
                onBatteryClick = {
                    activity?.let {
                        if (batteryUnrestricted) AppUtils.openAppSettings(it)
                        else AppUtils.requestIgnoreBatteryOptimizations(it)
                    }
                },
                notificationsEnabled = notificationsEnabled,
                onNotificationsClick = { activity?.let { AppUtils.openNotificationSettings(it) } },
                onHistoryClick = onHistoryClick
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun SettingsScreenPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        SettingsScreen(onBack = {}, onDeviceClick = {}, onHistoryClick = {})
    }
}
