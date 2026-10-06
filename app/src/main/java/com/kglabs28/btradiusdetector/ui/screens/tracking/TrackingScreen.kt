package com.kglabs28.btradiusdetector.ui.screens.tracking

import android.widget.Toast
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kglabs28.btradiusdetector.ui.components.HowToFindDialog
import com.kglabs28.btradiusdetector.ui.components.RadarTracker
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.ui.theme.SonarGreen
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

/**
 * Tracking screen. One [TrackingUiState] observation covers heading, signal,
 * battery, peak and history — no per-flow plumbing in the UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    deviceId: String,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: TrackingViewModel = viewModel(factory = TrackingViewModel.factory(context, deviceId))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showHelp by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            Strings.findingTitle(state.deviceName.ifEmpty { Strings.unknownDevice }),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = Dimens.textSectionSmall,
                            maxLines = 1
                        )
                        Text(
                            if (state.isConnected) Strings.connected else Strings.disconnected,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = Dimens.textCaptionSmall,
                            color = if (state.isConnected) SonarGreen else MaterialTheme.colorScheme.error
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = Strings.backDesc)
                    }
                },
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Rounded.Info, contentDescription = Strings.howToFindDesc)
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Rounded.Settings, contentDescription = Strings.settingsDesc)
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
        // Adaptive: narrow phones stack vertically; wide (tablet/foldable/landscape/car) splits.
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            val isWide = maxWidth >= 600.dp
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Dimens.screenPaddingHWide.scaled()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadarTracker(
                        heading = state.heading,
                        peakHeading = state.peakHeading,
                        peakRssi = state.peakRssi,
                        rssi = state.rssi,
                        history = state.history,
                        modifier = Modifier.weight(1f)
                    )
                    TrackingInfoSection(
                        state = state,
                        onBuzzClick = {
                            Toast.makeText(context, Strings.watchBuzzNeedsCompanion, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Dimens.screenPaddingHWide.scaled()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RadarTracker(
                        heading = state.heading,
                        peakHeading = state.peakHeading,
                        peakRssi = state.peakRssi,
                        rssi = state.rssi,
                        history = state.history,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacingSm.scaled()))
                    TrackingInfoSection(
                        state = state,
                        onBuzzClick = {
                            Toast.makeText(context, Strings.watchBuzzNeedsCompanion, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Dimens.screenPaddingHWide.scaled()))
                }
            }
        }
    }

    if (showHelp) {
        HowToFindDialog(onDismiss = { showHelp = false })
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun TrackingScreenPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Column(
                modifier = Modifier.padding(padding).fillMaxWidth()
                    .padding(horizontal = Dimens.screenPaddingHWide.scaled()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RadarTracker(heading = 42f, peakHeading = 42f, peakRssi = -52, rssi = -52)
                Spacer(modifier = Modifier.height(Dimens.spacingSm.scaled()))
                TrackingInfoSection(
                    state = TrackingUiState(
                        deviceName = "Keshav's Buds Pro",
                        heading = 42f, cardinal = "NE", rssi = -52,
                        battery = 72, peakRssi = -52, peakHeading = 42f
                    ),
                    onBuzzClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
