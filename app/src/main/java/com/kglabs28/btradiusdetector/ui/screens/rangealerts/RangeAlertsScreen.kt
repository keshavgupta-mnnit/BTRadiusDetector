package com.kglabs28.btradiusdetector.ui.screens.rangealerts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kglabs28.btradiusdetector.ui.components.DeviceAlertRow
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.utils.AppUtils
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

/**
 * Range Alerts device list. Only navigation primitives cross the boundary
 * ([onDeviceClick] carries just the address).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangeAlertsScreen(
    onBack: () -> Unit,
    onDeviceClick: (String) -> Unit
) {
    val viewModel: RangeAlertsViewModel = viewModel(factory = RangeAlertsViewModel.factory())
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(Strings.rangeAlertsTitle, fontWeight = FontWeight.Bold, fontSize = Dimens.textTitleSmall)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = Strings.backDesc)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .padding(horizontal = Dimens.screenPaddingH.scaled())
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                Strings.rangeAlertsSubtitle,
                fontSize = Dimens.textCaption,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.alphaSubtleText),
                modifier = Modifier.padding(
                    top = Dimens.spacingXs.scaled(),
                    bottom = Dimens.spacingMd.scaled()
                )
            )
            if (state.devices.isEmpty()) {
                Text(
                    Strings.noPairedDevicesShort,
                    fontSize = Dimens.textCaption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Dimens.spacingMd.scaled())
                )
            } else {
                state.devices.forEach { device ->
                    DeviceAlertRow(
                        icon = AppUtils.getDeviceIcon(device.deviceClass, device.minorDeviceClass),
                        deviceName = device.name ?: Strings.unknownDevice,
                        deviceType = AppUtils.getDeviceTypeLabel(device.deviceClass, device.minorDeviceClass),
                        checked = state.alertEnabled[device.address] ?: false,
                        onCheckedChange = { viewModel.setAlertEnabled(device.address, it) },
                        onClick = { onDeviceClick(device.address) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(Dimens.spacingLg.scaled()))
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun RangeAlertsScreenPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        RangeAlertsScreen(onBack = {}, onDeviceClick = {})
    }
}
