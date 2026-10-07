package com.kglabs28.btradiusdetector.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.kglabs28.btradiusdetector.BuildConfig
import com.kglabs28.btradiusdetector.domain.model.BluetoothDeviceModel
import com.kglabs28.btradiusdetector.utils.AppUtils
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

/** Header + monitored-device rows. Tapping a row opens its details screen. */
@Composable
fun RangeAlertsSection(
    devices: List<BluetoothDeviceModel>,
    alertEnabled: Map<String, Boolean>,
    onToggleAlert: (String, Boolean) -> Unit,
    onDeviceClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
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
        if (devices.isEmpty()) {
            Text(
                Strings.noPairedDevicesShort,
                fontSize = Dimens.textCaption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Dimens.spacingMd.scaled())
            )
        } else {
            devices.forEach { device ->
                DeviceAlertRow(
                    icon = AppUtils.getDeviceIcon(device.deviceClass, device.minorDeviceClass),
                    deviceName = device.name ?: Strings.unknownDevice,
                    deviceType = AppUtils.getDeviceTypeLabel(device.deviceClass, device.minorDeviceClass),
                    checked = alertEnabled[device.address] ?: device.isConnected,
                    onCheckedChange = { onToggleAlert(device.address, it) },
                    onClick = { onDeviceClick(device.address) }
                )
            }
        }
    }
}

/** Static about card plus the background-alerts battery gate. */
@Composable
fun AboutSection(
    batteryUnrestricted: Boolean,
    onBatteryClick: () -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
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
                icon = Icons.Rounded.NotificationsActive,
                title = Strings.notificationsTitle,
                value = if (notificationsEnabled) Strings.backgroundAlertsAllowed else Strings.backgroundAlertsRestricted,
                onClick = onNotificationsClick
            )
            SettingRow(
                icon = Icons.Rounded.Notifications,
                title = Strings.backgroundAlertsTitle,
                value = if (batteryUnrestricted) Strings.backgroundAlertsAllowed else Strings.backgroundAlertsRestricted,
                onClick = onBatteryClick
            )
            if (!batteryUnrestricted) {
                Text(
                    Strings.backgroundAlertsNote,
                    fontSize = Dimens.textCaptionSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText),
                    modifier = Modifier.padding(
                        start = Dimens.cardPaddingH.scaled(),
                        end = Dimens.cardPaddingH.scaled(),
                        bottom = Dimens.spacingSm.scaled()
                    )
                )
            }
            SettingRow(
                icon = Icons.Rounded.History,
                title = Strings.historyTitle,
                value = null,
                onClick = onHistoryClick
            )
            SettingRow(
                icon = Icons.Rounded.Info,
                title = Strings.aboutTitle,
                value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                showDivider = false,
                onClick = { }
            )
        }
        Spacer(modifier = Modifier.height(Dimens.spacingLg.scaled()))
    }
}
