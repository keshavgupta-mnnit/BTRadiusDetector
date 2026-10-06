package com.kglabs28.btradiusdetector.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kglabs28.btradiusdetector.ui.MainViewModel
import com.kglabs28.btradiusdetector.ui.components.AppCard
import com.kglabs28.btradiusdetector.ui.components.DeviceAlertRow
import com.kglabs28.btradiusdetector.ui.components.SettingRow
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.utils.AppUtils
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    // Settings owns its state via SettingsViewModel (UDF); MainViewModel param kept
    // for navigation compat and future range-alert service wiring.
    val context = LocalContext.current
    val settingsVm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(context))

    val bondedDevices by settingsVm.bondedDevices.collectAsStateWithLifecycle()
    val alertEnabled by settingsVm.alertEnabled.collectAsStateWithLifecycle()
    val sound by settingsVm.sound.collectAsStateWithLifecycle()
    val vibrationOn by settingsVm.vibrationOn.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { settingsVm.refresh() }

    SettingsContent(
        deviceRows = bondedDevices.map { device ->
            DeviceAlertUi(
                address = device.address,
                name = device.name ?: Strings.unknownDevice,
                type = AppUtils.getDeviceTypeLabel(device.deviceClass, device.minorDeviceClass),
                icon = AppUtils.getDeviceIcon(device.deviceClass, device.minorDeviceClass),
                checked = alertEnabled[device.address] ?: device.isConnected
            )
        },
        sound = sound,
        vibrationLabel = if (vibrationOn) Strings.vibrationOn else Strings.vibrationOff,
        onBack = onBack,
        onToggleAlert = { address, enabled -> settingsVm.setAlertEnabled(address, enabled) },
        onSoundClick = { settingsVm.toggleSound() },
        onVibrationClick = { settingsVm.toggleVibration() }
    )
}

/** Pure stateless content — previewable, adaptive via weight/fractions, no ViewModel coupling. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    deviceRows: List<DeviceAlertUi>,
    sound: String,
    vibrationLabel: String,
    onBack: () -> Unit,
    onToggleAlert: (String, Boolean) -> Unit,
    onSoundClick: () -> Unit,
    onVibrationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = Dimens.screenPaddingH.scaled())
        ) {
            item {
                Text(
                    Strings.rangeAlertsTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = Dimens.textSection,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = Dimens.spacingXs.scaled())
                )
                Text(
                    Strings.rangeAlertsSubtitle,
                    fontSize = Dimens.textCaption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText),
                    modifier = Modifier.padding(
                        top = Dimens.spacingXxs.scaled(),
                        bottom = Dimens.spacingMd.scaled()
                    )
                )
            }

            if (deviceRows.isEmpty()) {
                item {
                    Text(
                        Strings.noPairedDevicesShort,
                        fontSize = Dimens.textCaption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = Dimens.spacingMd.scaled())
                    )
                }
            } else {
                items(deviceRows, key = { it.address }) { row ->
                    DeviceAlertRow(
                        icon = row.icon,
                        deviceName = row.name,
                        deviceType = row.type,
                        checked = row.checked,
                        onCheckedChange = { onToggleAlert(row.address, it) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(Dimens.spacingMd.scaled()))
                Text(
                    Strings.otherSettingsTitle,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = Dimens.textSectionSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = Dimens.spacingSm.scaled())
                )
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    SettingRow(
                        icon = Icons.Rounded.Notifications,
                        title = Strings.notificationSoundTitle,
                        value = sound,
                        onClick = onSoundClick
                    )
                    SettingRow(
                        icon = Icons.Rounded.Vibration,
                        title = Strings.vibrationTitle,
                        value = vibrationLabel,
                        onClick = onVibrationClick
                    )
                    SettingRow(
                        icon = Icons.Rounded.Info,
                        title = Strings.aboutTitle,
                        value = null,
                        showDivider = false,
                        onClick = { }
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.spacingLg.scaled()))
            }
        }
    }
}

data class DeviceAlertUi(
    val address: String,
    val name: String,
    val type: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val checked: Boolean
)

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun SettingsContentPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        SettingsContent(
            deviceRows = emptyList(),
            sound = Strings.notificationSoundDefault,
            vibrationLabel = Strings.vibrationOn,
            onBack = {},
            onToggleAlert = { _, _ -> },
            onSoundClick = {},
            onVibrationClick = {}
        )
    }
}
