package com.kglabs28.btradiusdetector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.kglabs28.btradiusdetector.domain.model.AlertSoundMode
import com.kglabs28.btradiusdetector.ui.screens.alertdetails.AlertDetailsEvent
import com.kglabs28.btradiusdetector.ui.screens.alertdetails.AlertDetailsUiState
import com.kglabs28.btradiusdetector.ui.theme.ContentWhite
import com.kglabs28.btradiusdetector.ui.theme.SonarGreen
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

/** Device header with master monitoring toggle. */
@Composable
fun DeviceHeaderCard(
    state: AlertDetailsUiState,
    icon: ImageVector,
    onEvent: (AlertDetailsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(
                horizontal = Dimens.cardPaddingH.scaled(),
                vertical = Dimens.cardPaddingV.scaled()
            ).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(Dimens.iconCircleSizeSetting.scaled()).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = Dimens.alphaCardTint)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(Dimens.iconSizeRow.scaled())
                )
            }
            Spacer(modifier = Modifier.width(Dimens.spacingMd.scaled()))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    state.deviceName, fontWeight = FontWeight.SemiBold, fontSize = Dimens.textBodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(Dimens.spacingXs.scaled()).clip(CircleShape)
                            .background(if (state.isConnected) SonarGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.width(Dimens.spacingXs.scaled()))
                    Text(
                        if (state.isConnected) Strings.connected else Strings.paired,
                        fontSize = Dimens.textCaptionSmall,
                        color = if (state.isConnected) SonarGreen
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText)
                    )
                }
            }
            Switch(
                checked = state.monitoringEnabled,
                onCheckedChange = { onEvent(AlertDetailsEvent.MonitoringToggled(it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ContentWhite,
                    checkedTrackColor = SonarGreen,
                    uncheckedThumbColor = ContentWhite.copy(alpha = Dimens.alphaMuted),
                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaTrackOff)
                )
            )
        }
    }
}

/** The four per-device notification toggles. */
@Composable
fun AlertTogglesCard(
    state: AlertDetailsUiState,
    onEvent: (AlertDetailsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        SwitchSettingRow(
            title = Strings.notifyOnDisconnect,
            checked = state.notifyOnDisconnect,
            onCheckedChange = { onEvent(AlertDetailsEvent.NotifyDisconnectToggled(it)) }
        )
        SwitchSettingRow(
            title = Strings.notifyOnReconnect,
            checked = state.notifyOnReconnect,
            onCheckedChange = { onEvent(AlertDetailsEvent.NotifyReconnectToggled(it)) }
        )
        SoundModeRow(
            mode = state.soundMode,
            onModeSelected = { onEvent(AlertDetailsEvent.SoundModeSelected(it)) }
        )
        SwitchSettingRow(
            title = Strings.vibrationTitle,
            checked = state.vibrationEnabled,
            onCheckedChange = { onEvent(AlertDetailsEvent.VibrationToggled(it)) },
            showDivider = false
        )
    }
}

/** Notification sound as Off / One time / Continuous picker dialog. */
@Composable
fun SoundModeRow(
    mode: AlertSoundMode,
    onModeSelected: (AlertSoundMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    fun label(m: AlertSoundMode): String = when (m) {
        AlertSoundMode.OFF -> Strings.soundModeOff
        AlertSoundMode.ONCE -> Strings.soundModeOnce
        AlertSoundMode.CONTINUOUS -> Strings.soundModeContinuous
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = { showDialog = true })
                .padding(horizontal = Dimens.cardPaddingH.scaled(), vertical = Dimens.cardPaddingV.scaled()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                Strings.notificationSoundTitle, fontSize = Dimens.textLabel,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                label(mode), fontSize = Dimens.textCaption,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText)
            )
            Spacer(modifier = Modifier.width(Dimens.spacingSm.scaled()))
            Icon(
                Icons.Rounded.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText),
                modifier = Modifier.size(Dimens.iconChevronSize.scaled())
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth()
                .padding(start = Dimens.cardPaddingH.scaled())
                .height(Dimens.dividerH.scaled())
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = Dimens.alphaDivider))
        )
    }

    if (showDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(Strings.notificationSoundTitle) },
            text = {
                Column {
                    AlertSoundMode.values().forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onModeSelected(option)
                                showDialog = false
                            }.padding(vertical = Dimens.spacingSm.scaled()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = option == mode,
                                onClick = {
                                    onModeSelected(option)
                                    showDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(Dimens.spacingSm.scaled()))
                            Text(label(option), fontSize = Dimens.textLabel)
                        }
                    }
                }
            },
            confirmButton = { }
        )
    }
}
