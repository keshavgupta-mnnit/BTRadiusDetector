package com.kglabs28.btradiusdetector.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kglabs28.btradiusdetector.domain.model.BluetoothDeviceModel
import com.kglabs28.btradiusdetector.ui.components.DeviceRow
import com.kglabs28.btradiusdetector.ui.components.MonitorRangeAlertsCard
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

/** Paired-device list with adaptive insets and the monitor card footer. */
@Composable
fun PairedDeviceList(
    devices: List<BluetoothDeviceModel>,
    onDeviceSelected: (String) -> Unit,
    onMonitorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Adaptive padding: wider screens get larger horizontal insets (tablet/foldable/car).
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 600.dp
        val horizontal = if (isWide) Dimens.screenPaddingHWide.scaled() else Dimens.screenPaddingH.scaled()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = horizontal, vertical = Dimens.spacingSm.scaled()),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd.scaled())
        ) {
            item {
                Text(
                    text = Strings.pairedDevicesHeader,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = Dimens.textSectionSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(
                        top = Dimens.spacingXs.scaled(),
                        bottom = Dimens.spacingXxs.scaled(),
                        start = Dimens.spacingXxs.scaled()
                    )
                )
            }
            items(devices, key = { it.address }) { device ->
                DeviceRow(device = device, onClick = { if (device.isConnected) onDeviceSelected(device.address) })
            }
            item {
                Spacer(modifier = Modifier.height(Dimens.spacingXs.scaled()))
                MonitorRangeAlertsCard(onClick = onMonitorClick)
            }
        }
    }
}
