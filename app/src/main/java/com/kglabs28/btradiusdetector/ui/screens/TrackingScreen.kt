package com.kglabs28.btradiusdetector.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Watch
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kglabs28.btradiusdetector.ui.MainViewModel
import com.kglabs28.btradiusdetector.ui.components.CompassRing
import com.kglabs28.btradiusdetector.ui.components.HeadingCard
import com.kglabs28.btradiusdetector.ui.components.HowToFindDialog
import com.kglabs28.btradiusdetector.ui.components.OutlineActionButton
import com.kglabs28.btradiusdetector.ui.components.PulsingDot
import com.kglabs28.btradiusdetector.ui.components.RadarView
import com.kglabs28.btradiusdetector.ui.components.SignalStrengthCard
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.ui.theme.SonarGreen
import com.kglabs28.btradiusdetector.utils.Constants
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.SignalUtils
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    deviceId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val heading by viewModel.headingFlow.collectAsStateWithLifecycle(initialValue = 0f)
    val cardinal by viewModel.headingCardinal.collectAsStateWithLifecycle(initialValue = "N")
    val rssiFlow = remember(deviceId) { viewModel.getRssiFlow(deviceId) }
    val rssi by rssiFlow.collectAsStateWithLifecycle(initialValue = Constants.RSSI_FLOOR)

    val peakRssi by viewModel.peakRssi.collectAsStateWithLifecycle()
    val peakHeading by viewModel.peakHeading.collectAsStateWithLifecycle()
    val signalHistory by viewModel.signalHistory.collectAsStateWithLifecycle()
    val trackingDevice by viewModel.trackingDevice.collectAsStateWithLifecycle()

    val deviceName = trackingDevice?.name ?: Strings.unknownDevice
    val isConnected = trackingDevice?.isConnected ?: true

    var showHelp by remember { mutableStateOf(false) }

    LaunchedEffect(deviceId) { viewModel.selectDevice(deviceId) }
    LaunchedEffect(Unit) { viewModel.refreshBondedDevices() }
    LaunchedEffect(rssi, heading) { viewModel.updateSignalData(rssi, heading) }

    val animatedHeading by animateFloatAsState(
        targetValue = heading,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = Strings.signalStrength
    )
    val dotScale = SignalUtils.dotScale(rssi)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            Strings.findingTitle(deviceName),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = Dimens.textSectionSmall,
                            maxLines = 1
                        )
                        Text(
                            if (isConnected) Strings.connected else Strings.disconnected,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = Dimens.textCaptionSmall,
                            color = if (isConnected) SonarGreen else MaterialTheme.colorScheme.error
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.clearSelection(); onBack() }) {
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
                    Column(modifier = Modifier.weight(1f)) {
                        RadarBlock(
                            heading = animatedHeading,
                            peakHeading = peakHeading,
                            peakRssi = peakRssi,
                            dotScale = dotScale,
                            currentHeading = heading,
                            history = signalHistory,
                            modifier = Modifier.fillMaxWidth()
                        )
                        GuidanceLine(
                            peakHeading = peakHeading,
                            peakRssi = peakRssi,
                            heading = heading
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        SignalStrengthCard(rssi = rssi)
                        Spacer(modifier = Modifier.height(Dimens.spacingMd.scaled()))
                        HeadingCard(heading = heading, cardinal = cardinal)
                        Spacer(modifier = Modifier.height(Dimens.spacingMd.scaled()))
                        OutlineActionButton(
                            label = Strings.buzzMyWatch,
                            leadingIcon = Icons.Rounded.Watch,
                            onClick = {
                                Toast.makeText(context, Strings.watchBuzzNeedsCompanion, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Dimens.screenPaddingHWide.scaled()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RadarBlock(
                        heading = animatedHeading,
                        peakHeading = peakHeading,
                        peakRssi = peakRssi,
                        dotScale = dotScale,
                        currentHeading = heading,
                        history = signalHistory,
                        modifier = Modifier.fillMaxWidth()
                    )
                    GuidanceLine(
                        peakHeading = peakHeading,
                        peakRssi = peakRssi,
                        heading = heading
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacingSm.scaled()))
                    SignalStrengthCard(rssi = rssi)
                    Spacer(modifier = Modifier.height(Dimens.spacingMd.scaled()))
                    HeadingCard(heading = heading, cardinal = cardinal)
                    Spacer(modifier = Modifier.height(Dimens.spacingMd.scaled()))
                    OutlineActionButton(
                        label = Strings.buzzMyWatch,
                        leadingIcon = Icons.Rounded.Watch,
                        onClick = {
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

@Composable
private fun RadarBlock(
    heading: Float,
    peakHeading: Float,
    peakRssi: Int,
    dotScale: Float,
    currentHeading: Float,
    history: List<com.kglabs28.btradiusdetector.ui.SignalPoint>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.height(Dimens.radarBoxHeight.scaled()),
        contentAlignment = Alignment.Center
    ) {
        RadarView(
            peakHeading = peakHeading,
            peakRssi = peakRssi,
            currentHeading = currentHeading,
            history = history
        )
        CompassRing(heading = heading)
        PulsingDot(scale = dotScale)
        if (peakRssi > Constants.RSSI_FLOOR) {
            Text(
                text = SignalUtils.formattedBestSignal(peakHeading),
                color = SonarGreen,
                fontWeight = FontWeight.SemiBold,
                fontSize = Dimens.textBestSignal,
                textAlign = TextAlign.End,
                lineHeight = Dimens.lineHeightBestSignal,
                modifier = Modifier.align(Alignment.TopEnd)
                    .padding(top = Dimens.spacingSm.scaled(), end = Dimens.spacingXs.scaled())
            )
        }
    }
}

@Composable
private fun GuidanceLine(
    peakHeading: Float,
    peakRssi: Int,
    heading: Float,
    modifier: Modifier = Modifier
) {
    if (peakRssi <= Constants.RSSI_FLOOR) return
    Text(
        text = SignalUtils.turnGuidance(peakHeading, heading),
        color = SonarGreen,
        fontWeight = FontWeight.SemiBold,
        fontSize = Dimens.textCaption,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
            .padding(vertical = Dimens.spacingXs.scaled())
    )
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun TrackingScreenPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.screenPaddingHWide.scaled()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RadarBlock(
                        heading = 42f, peakHeading = 42f, peakRssi = -52,
                        dotScale = 0.85f, currentHeading = 42f, history = emptyList(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Dimens.spacingSm.scaled()))
                    SignalStrengthCard(rssi = -52)
                    Spacer(modifier = Modifier.height(Dimens.spacingMd.scaled()))
                    HeadingCard(heading = 42f, cardinal = "NE")
                }
            }
        }
    }
}
