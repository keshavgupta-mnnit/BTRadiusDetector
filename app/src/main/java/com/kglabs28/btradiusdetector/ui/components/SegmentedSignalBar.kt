package com.kglabs28.btradiusdetector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.utils.Constants
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.domain.signal.SignalEngine
import com.kglabs28.btradiusdetector.utils.scaled

/**
 * Segmented signal bar. Stateless — [rssi] in, no ViewModel coupling.
 */
@Composable
fun SegmentedSignalBar(
    rssi: Int,
    modifier: Modifier = Modifier,
    segments: Int = Constants.SIGNAL_SEGMENTS
) {
    val filled = SignalEngine.filledSegments(rssi, segments)
    val barColor = SignalEngine.proximityColor(rssi)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.signalBarSpacing.scaled())
    ) {
        repeat(segments) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(
                        if (i < filled) Dimens.signalBarFilledH.scaled()
                        else Dimens.signalBarEmptyH.scaled()
                    )
                    .align(Alignment.CenterVertically)
                    .clip(RoundedCornerShape(Dimens.cornerRadiusSegment.scaled()))
                    .background(
                        if (i < filled) barColor else barColor.copy(alpha = Dimens.alphaSegmentOff)
                    )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SegmentedSignalBarPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        SegmentedSignalBar(rssi = -52)
    }
}
