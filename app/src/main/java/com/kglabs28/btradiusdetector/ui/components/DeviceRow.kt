package com.kglabs28.btradiusdetector.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.kglabs28.btradiusdetector.domain.model.BluetoothDeviceModel
import com.kglabs28.btradiusdetector.utils.AppUtils
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.scaled

@Composable
fun DeviceRow(
    device: BluetoothDeviceModel,
    onClick: () -> Unit
) {
    val contentAlpha = if (device.isConnected) 1f else Dimens.alphaDisabledContent

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = device.isConnected, onClick = onClick),
        shape = RoundedCornerShape(Dimens.cornerRadiusCard.scaled()),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Dimens.elevationCard.scaled()
    ) {
        Row(
            modifier = Modifier
                .padding(Dimens.spacingMd.scaled())
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.iconCircleSize.scaled())
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = contentAlpha)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppUtils.getDeviceIcon(device.deviceClass, device.minorDeviceClass),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = contentAlpha)
                )
            }

            Spacer(modifier = Modifier.width(Dimens.spacingMd.scaled()))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "Unknown Device",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
                Text(
                    text = "${AppUtils.getDeviceTypeLabel(device.deviceClass, device.minorDeviceClass)} • ${if (device.isConnected) "Connected" else "Paired"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (device.isConnected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText)
                )
            }

            Icon(
                imageVector = Icons.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconArrowSize.scaled()),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
            )
        }
    }
}