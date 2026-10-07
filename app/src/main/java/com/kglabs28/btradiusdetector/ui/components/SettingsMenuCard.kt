package com.kglabs28.btradiusdetector.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.rounded.Info
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

/**
 * Big bordered menu card: icon disc, bold title, gray subtitle, chevron.
 */
@Composable
fun SettingsMenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.cornerRadiusDialog.scaled()),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            Dimens.borderWidthThin.scaled(),
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = Dimens.alphaBorderSubtle)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(
                    horizontal = Dimens.cardPaddingH.scaled(),
                    vertical = Dimens.cardPaddingVLarge.scaled()
                )
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.iconCircleSize.scaled())
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = Dimens.alphaCardTint)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(Dimens.iconSizeCard.scaled())
                )
            }

            Spacer(modifier = Modifier.width(Dimens.spacingMd.scaled()))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = Dimens.textCardTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = Dimens.textCaption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText)
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconChevronSizeSmall.scaled()),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsMenuCardPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        SettingsMenuCard(
            icon = Icons.Rounded.Info,
            title = Strings.aboutTitle,
            subtitle = Strings.aboutBody,
            onClick = {}
        )
    }
}
