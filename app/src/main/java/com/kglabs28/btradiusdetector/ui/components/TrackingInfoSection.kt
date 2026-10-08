package com.kglabs28.btradiusdetector.ui.components

import com.kglabs28.btradiusdetector.ui.screens.tracking.TrackingUiState

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.domain.signal.SignalEngine
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

/** Signal + heading + battery + buzz cards stacked under the radar. */
@Composable
fun TrackingInfoSection(
    state: TrackingUiState,
    onBuzzClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SignalStrengthCard(rssi = state.rssi)
        Spacer(modifier = Modifier.height(Dimens.spacingMd.scaled()))
        HeadingCard(heading = state.heading, cardinal = state.cardinal)
        state.battery?.let { battery ->
            // A frozen percentage on a dead link misleads — show it only live.
            if (state.isConnected) {
                Spacer(modifier = Modifier.height(Dimens.spacingMd.scaled()))
                BatteryCard(percent = battery)
            }
        }
        // Buzz surface is type-driven, never hardcoded per device: only
        // classes with a buzzable companion show the button, with their label.
        SignalEngine.buzzLabelFor(state.majorClass)?.let { buzzLabel ->
            Spacer(modifier = Modifier.height(Dimens.spacingMd.scaled()))
            OutlineActionButton(
                label = buzzLabel,
                leadingIcon = Icons.Rounded.Watch,
                onClick = onBuzzClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
