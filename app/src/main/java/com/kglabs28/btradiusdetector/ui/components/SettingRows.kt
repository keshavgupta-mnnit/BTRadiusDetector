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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.ui.theme.ContentWhite
import com.kglabs28.btradiusdetector.ui.theme.SonarGreen
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.scaled

/**
 * Generic icon + title + optional value + chevron row. Stateless, slot for trailing.
 */
@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    showDivider: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
                .padding(horizontal = Dimens.cardPaddingH.scaled(), vertical = Dimens.cardPaddingV.scaled()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimens.iconSizeRow.scaled())
            )
            Spacer(modifier = Modifier.width(Dimens.spacingMd.scaled()))
            Text(
                title, fontSize = Dimens.textLabel,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (value != null) {
                Text(
                    value, fontSize = Dimens.textCaption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText)
                )
                Spacer(modifier = Modifier.width(Dimens.spacingSm.scaled()))
            }
            if (trailingContent != null) trailingContent()
            else Icon(
                Icons.Rounded.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText),
                modifier = Modifier.size(Dimens.iconChevronSize.scaled())
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = Dimens.dividerStartInset.scaled())
                    .height(Dimens.dividerH.scaled())
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = Dimens.alphaDivider))
            )
        }
    }
}

/**
 * Device alert toggle row. Stateless — [checked] + [onCheckedChange] hoisted.
 * Tapping the row (not the switch) fires [onClick] when provided.
 */
@Composable
fun DeviceAlertRow(
    icon: ImageVector,
    deviceName: String,
    deviceType: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    AppCard(modifier = modifier.fillMaxWidth().padding(vertical = Dimens.spacingXs.scaled())) {
        Row(
            modifier = Modifier
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(
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
                    deviceName, fontWeight = FontWeight.SemiBold, fontSize = Dimens.textBodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    deviceType, fontSize = Dimens.textCaptionSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText)
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
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

/**
 * Plain title + switch row for the Alert Settings card. Stateless.
 */
@Composable
fun SwitchSettingRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = Dimens.cardPaddingH.scaled(), vertical = Dimens.spacingSm.scaled()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title, fontSize = Dimens.textLabel,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ContentWhite,
                    checkedTrackColor = SonarGreen,
                    uncheckedThumbColor = ContentWhite.copy(alpha = Dimens.alphaMuted),
                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaTrackOff)
                )
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = Dimens.cardPaddingH.scaled())
                    .height(Dimens.dividerH.scaled())
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = Dimens.alphaDivider))
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingRowsPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        Column {
            SettingRow(
                icon = Icons.Rounded.ChevronRight, title = "About",
                value = null, showDivider = false, onClick = {}
            )
            SwitchSettingRow(title = "Vibration", checked = true, onCheckedChange = {})
        }
    }
}
