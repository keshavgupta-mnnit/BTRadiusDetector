package com.kglabs28.btradiusdetector.ui.screens.alertdetails

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kglabs28.btradiusdetector.ui.components.AppCard
import com.kglabs28.btradiusdetector.ui.components.SwitchSettingRow
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.utils.AppUtils
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

/**
 * Per-device Range Alerts details. One state observation, one event handler —
 * only the device address crosses the navigation boundary.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceAlertDetailsScreen(
    deviceId: String,
    onBack: () -> Unit
) {
    val viewModel: DeviceAlertDetailsViewModel =
        viewModel(factory = DeviceAlertDetailsViewModel.factory(deviceId))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
        ) {
            DeviceHeaderCard(
                state = state,
                icon = AppUtils.getDeviceIcon(state.majorClass, state.minorClass),
                onEvent = viewModel::onEvent
            )

            Spacer(modifier = Modifier.height(Dimens.spacingMd.scaled()))

            Text(
                Strings.alertSettingsTitle,
                fontWeight = FontWeight.SemiBold,
                fontSize = Dimens.textSectionSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = Dimens.spacingSm.scaled())
            )
            AlertTogglesCard(
                state = state,
                onEvent = viewModel::onEvent
            )

            Spacer(modifier = Modifier.height(Dimens.spacingLg.scaled()))

            Text(
                Strings.alertDetailsCaption,
                fontSize = Dimens.textCaption,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun DeviceAlertDetailsPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        DeviceAlertDetailsScreen(deviceId = "00:00:00:00:00:00", onBack = {})
    }
}
