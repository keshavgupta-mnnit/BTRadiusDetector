package com.kglabs28.btradiusdetector.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kglabs28.btradiusdetector.BuildConfig
import com.kglabs28.btradiusdetector.ui.components.SettingsMenuCard
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.utils.AppUtils
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

/**
 * Settings menu in the reference style: one bordered card per area.
 * Status rows re-check on every resume (back from system settings).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onRangeAlertsClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
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
    val notificationsEnabled = remember(resumeTick) { AppUtils.areNotificationsEnabled(context) }

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
                .padding(horizontal = Dimens.screenPaddingH.scaled()),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd.scaled())
        ) {
            Spacer(modifier = Modifier.height(Dimens.spacingXs.scaled()))

            SettingsMenuCard(
                icon = Icons.Rounded.NotificationsActive,
                title = Strings.rangeAlertsTitle,
                subtitle = Strings.rangeAlertsMenuSubtitle,
                onClick = onRangeAlertsClick
            )
            SettingsMenuCard(
                icon = Icons.Rounded.Notifications,
                title = Strings.notificationsTitle,
                subtitle = if (notificationsEnabled) Strings.backgroundAlertsAllowed else Strings.backgroundAlertsRestricted,
                onClick = { activity?.let { AppUtils.openNotificationSettings(it) } }
            )
            SettingsMenuCard(
                icon = Icons.Rounded.Bolt,
                title = Strings.backgroundAlertsTitle,
                subtitle = if (batteryUnrestricted) Strings.backgroundAlertsAllowed else Strings.backgroundAlertsNote,
                onClick = {
                    activity?.let {
                        if (batteryUnrestricted) AppUtils.openAppSettings(it)
                        else AppUtils.requestIgnoreBatteryOptimizations(it)
                    }
                }
            )
            SettingsMenuCard(
                icon = Icons.Rounded.History,
                title = Strings.historyTitle,
                subtitle = Strings.historyMenuSubtitle,
                onClick = onHistoryClick
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                Strings.appVersionLabel(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                fontSize = Dimens.textCaption,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = Dimens.spacingLg.scaled())
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun SettingsScreenPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        SettingsScreen(onBack = {}, onRangeAlertsClick = {}, onHistoryClick = {})
    }
}
