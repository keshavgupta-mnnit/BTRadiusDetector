package com.kglabs28.btradiusdetector.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.kglabs28.btradiusdetector.domain.model.AlertRepeatMode
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

/** The per-device notification toggles plus the repetition dropdown. */
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
        SwitchSettingRow(
            title = Strings.notificationSoundTitle,
            checked = state.soundEnabled,
            onCheckedChange = { onEvent(AlertDetailsEvent.SoundToggled(it)) }
        )
        SwitchSettingRow(
            title = Strings.vibrationTitle,
            checked = state.vibrationEnabled,
            onCheckedChange = { onEvent(AlertDetailsEvent.VibrationToggled(it)) },
            showDivider = false
        )
    }

    Spacer(modifier = Modifier.height(Dimens.spacingMd.scaled()))

    // Repetition is meaningless for a channels-off visual ping.
    val channelsOff = !state.soundEnabled && !state.vibrationEnabled
    AlertRepeatDropdown(
        mode = state.repeatMode,
        enabled = !channelsOff,
        onModeSelected = { onEvent(AlertDetailsEvent.RepeatModeSelected(it)) }
    )
}

/** Alert repetition as an inline dropdown: Once / Beep. */
@Composable
fun AlertRepeatDropdown(
    mode: AlertRepeatMode,
    enabled: Boolean,
    onModeSelected: (AlertRepeatMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    fun label(m: AlertRepeatMode): String = when (m) {
        AlertRepeatMode.ONCE -> Strings.repeatOnce
        AlertRepeatMode.CONTINUOUS -> Strings.repeatContinuous
    }

    fun icon(m: AlertRepeatMode): ImageVector = when (m) {
        AlertRepeatMode.ONCE -> Icons.AutoMirrored.Rounded.VolumeUp
        AlertRepeatMode.CONTINUOUS -> Icons.Rounded.NotificationsActive
    }

    val contentAlpha = if (enabled) 1f else Dimens.alphaDisabledContent

    Column(modifier = modifier.alpha(contentAlpha)) {
        Text(
            Strings.alertTypeTitle,
            fontWeight = FontWeight.SemiBold,
            fontSize = Dimens.textSectionSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = contentAlpha),
            modifier = Modifier.padding(bottom = Dimens.spacingSm.scaled())
        )
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = { expanded = !expanded }),
            shape = RoundedCornerShape(Dimens.cornerRadiusPill.scaled()),
            color = Color.Transparent,
            border = BorderStroke(
                Dimens.borderWidthThin.scaled(),
                MaterialTheme.colorScheme.primary.copy(alpha = Dimens.alphaBorderSubtle)
            )
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = Dimens.cardPaddingH.scaled(),
                    vertical = Dimens.spacingSm.scaled()
                ).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon(mode),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText),
                    modifier = Modifier.size(Dimens.iconSizeRow.scaled())
                )
                Spacer(modifier = Modifier.width(Dimens.spacingMd.scaled()))
                Text(
                    text = label(mode),
                    fontSize = Dimens.textBody,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(Dimens.iconSizeRow.scaled())
                )
            }
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(Dimens.spacingSm.scaled()))
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = Dimens.spacingSm.scaled())) {
                    AlertRepeatMode.values().forEach { option ->
                        val selected = option == mode
                        val accent = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText)
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onModeSelected(option)
                                expanded = false
                            }.padding(
                                horizontal = Dimens.cardPaddingH.scaled(),
                                vertical = Dimens.spacingSm.scaled()
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(Dimens.iconCircleSizeSmall.scaled()).clip(CircleShape)
                                    .border(
                                        Dimens.borderWidthThin.scaled(),
                                        accent,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon(option),
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(Dimens.iconSizeSmall.scaled())
                                )
                            }
                            Spacer(modifier = Modifier.width(Dimens.spacingMd.scaled()))
                            Text(
                                text = label(option),
                                fontSize = Dimens.textBody,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(Dimens.iconSizeRow.scaled())
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
