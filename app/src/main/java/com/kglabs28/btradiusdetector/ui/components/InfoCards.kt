package com.kglabs28.btradiusdetector.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.ui.theme.SonarGreen
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.SignalUtils
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

/**
 * Signal strength card. Stateless — [rssi] in only.
 */
@Composable
fun SignalStrengthCard(rssi: Int, modifier: Modifier = Modifier) {
    val label = SignalUtils.proximityLabel(rssi)
    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(
                horizontal = Dimens.cardPaddingH.scaled(),
                vertical = Dimens.cardPaddingVLarge.scaled()
            ).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Wifi,
                contentDescription = null,
                tint = SonarGreen,
                modifier = Modifier.size(Dimens.iconSignal.scaled())
            )
            Spacer(modifier = Modifier.width(Dimens.spacingMd.scaled()))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    Strings.signalStrength,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = Dimens.textTiny,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText)
                )
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = Dimens.textCardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                SignalUtils.formattedDbm(rssi),
                style = MaterialTheme.typography.labelMedium,
                fontSize = Dimens.textCaptionSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText)
            )
        }
        SegmentedSignalBar(
            rssi = rssi,
            modifier = Modifier.padding(
                start = Dimens.cardPaddingH.scaled(),
                end = Dimens.cardPaddingH.scaled(),
                bottom = Dimens.cardPaddingVLarge.scaled()
            )
        )
    }
}

/**
 * Current heading card. Stateless — [heading] in only.
 */
@Composable
fun HeadingCard(heading: Float, cardinal: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(
                horizontal = Dimens.cardPaddingH.scaled(),
                vertical = Dimens.cardPaddingV.scaled()
            ).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Explore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimens.iconHeading.scaled())
            )
            Spacer(modifier = Modifier.width(Dimens.spacingMd.scaled()))
            Column {
                Text(
                    Strings.currentHeading,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = Dimens.textTiny,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText)
                )
                Text(
                    Strings.headingLabel(heading.toInt(), cardinal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = Dimens.textCardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InfoCardsPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        Column {
            SignalStrengthCard(rssi = -52)
            HeadingCard(heading = 42f, cardinal = "NE")
        }
    }
}
