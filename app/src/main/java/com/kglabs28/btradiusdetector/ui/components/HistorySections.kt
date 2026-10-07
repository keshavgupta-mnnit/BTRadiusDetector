package com.kglabs28.btradiusdetector.ui.components

import com.kglabs28.btradiusdetector.ui.screens.history.DeviceFilter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.kglabs28.btradiusdetector.domain.model.AlertActivity
import com.kglabs28.btradiusdetector.ui.components.AppCard
import com.kglabs28.btradiusdetector.ui.theme.SonarGreen
import com.kglabs28.btradiusdetector.utils.AppUtils
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

/** Device filter chips: All + one per seen device. */
@Composable
fun HistoryFilterChips(
    devices: List<DeviceFilter>,
    selectedAddress: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm.scaled())
    ) {
        items(devices, key = { it.address ?: "all" }) { device ->
            FilterChip(
                selected = selectedAddress == device.address,
                onClick = { onSelect(device.address) },
                label = { Text(device.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SonarGreen.copy(alpha = 0.2f),
                    selectedLabelColor = SonarGreen
                )
            )
        }
    }
}

/** Newest-first rows: device icon, name, event + status, time. */
@Composable
fun HistoryList(
    entries: List<AlertActivity>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (entries.isEmpty()) {
            Text(
                Strings.noHistory,
                fontSize = Dimens.textCaption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Dimens.spacingMd.scaled())
            )
        } else {
            entries.forEachIndexed { index, entry ->
                AppCard(modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacingXs.scaled())) {
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
                                AppUtils.getDeviceIcon(entry.majorClass, entry.minorClass),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(Dimens.iconSizeRow.scaled())
                            )
                        }
                        Spacer(modifier = Modifier.width(Dimens.spacingMd.scaled()))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                entry.deviceName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = Dimens.textBodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                entry.event,
                                fontSize = Dimens.textCaptionSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                if (entry.posted) Strings.activityNotified else Strings.activityAvoided,
                                fontSize = Dimens.textCaptionSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (entry.posted) SonarGreen
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText)
                            )
                            Text(
                                AppUtils.formatElapsedTime(entry.atMillis),
                                fontSize = Dimens.textCaptionSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText)
                            )
                        }
                    }
                }
                if (index < entries.lastIndex) {
                    Spacer(modifier = Modifier.height(Dimens.spacingXs.scaled()))
                }
            }
        }
    }
}
