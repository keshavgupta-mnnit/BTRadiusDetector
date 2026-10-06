package com.kglabs28.btradiusdetector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.ui.theme.SonarGreen
import com.kglabs28.btradiusdetector.utils.AppUtils
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

/**
 * Paired-device row. Stateless — [device] in, [onClick] out. Previewable.
 */
@Composable
fun DeviceRow(
    device: com.kglabs28.btradiusdetector.domain.model.BluetoothDeviceModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connected = device.isConnected
    val contentAlpha = if (connected) 1f else Dimens.alphaDisabledContent

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (connected) Modifier.border(
                    Dimens.borderWidthThin.scaled(),
                    SonarGreen.copy(alpha = Dimens.alphaBuzzBorder),
                    RoundedCornerShape(Dimens.cornerRadiusCard.scaled())
                ) else Modifier
            )
            .clickable(enabled = connected, onClick = onClick),
        shape = RoundedCornerShape(Dimens.cornerRadiusCard.scaled()),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Dimens.elevationNone.scaled()
    ) {
        Row(
            modifier = Modifier
                .padding(
                    horizontal = Dimens.cardPaddingH.scaled(),
                    vertical = Dimens.cardPaddingV.scaled()
                )
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.iconCircleSizeRow.scaled())
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = Dimens.alphaCardTint)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppUtils.getDeviceIcon(device.deviceClass, device.minorDeviceClass),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = contentAlpha),
                    modifier = Modifier.size(Dimens.iconSizeCard.scaled())
                )
            }

            Spacer(modifier = Modifier.width(Dimens.spacingMd.scaled()))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: Strings.unknownDevice,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = Dimens.textBody,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
                Text(
                    text = "${AppUtils.getDeviceTypeLabel(device.deviceClass, device.minorDeviceClass)} • ${if (connected) Strings.connected else Strings.paired}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = Dimens.textCaption,
                    color = if (connected)
                        SonarGreen
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText)
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconChevronSizeSmall.scaled()),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeviceRowPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        DeviceRow(
            device = com.kglabs28.btradiusdetector.domain.model.BluetoothDeviceModel(
                address = "00:00:00:00:00:00",
                name = "Keshav's Buds Pro",
                deviceClass = android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO,
                minorDeviceClass = 0,
                isConnected = true
            ),
            onClick = {}
        )
    }
}
