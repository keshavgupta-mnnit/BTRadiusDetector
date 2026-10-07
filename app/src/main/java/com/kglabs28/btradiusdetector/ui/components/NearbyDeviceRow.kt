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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.kglabs28.btradiusdetector.domain.model.NearbyDevice
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.ui.theme.SonarGreen
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.SignalUtils
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

/**
 * One overheard radio: name (or address), live RSSI. Always tappable —
 * unknown buds have no connection state to gate on.
 */
@Composable
fun NearbyDeviceRow(
    device: NearbyDevice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBonded: Boolean = false
) {
    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                    imageVector = Icons.Rounded.Bluetooth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(Dimens.iconSizeCard.scaled())
                )
            }

            Spacer(modifier = Modifier.width(Dimens.spacingMd.scaled()))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name ?: Strings.unknownDevice,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = Dimens.textBody,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isBonded) {
                        Spacer(modifier = Modifier.width(Dimens.spacingSm.scaled()))
                        Text(
                            text = Strings.paired,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = Dimens.textTiny,
                            color = SonarGreen
                        )
                    }
                }
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = Dimens.textCaptionSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = SignalUtils.formattedDbm(device.rssi),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = Dimens.textCaption,
                    color = SonarGreen
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconChevronSizeSmall.scaled()),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NearbyDeviceRowPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        NearbyDeviceRow(
            device = NearbyDevice("28:6F:40:D8:C5:4E", "OPPO Enco Air3 Pro", -52),
            onClick = {}
        )
    }
}
