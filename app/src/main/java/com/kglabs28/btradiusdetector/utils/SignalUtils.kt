package com.kglabs28.btradiusdetector.utils

import android.bluetooth.BluetoothClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kglabs28.btradiusdetector.ui.theme.ProximityCold
import com.kglabs28.btradiusdetector.ui.theme.ProximityHot
import com.kglabs28.btradiusdetector.ui.theme.ProximityWarm
import com.kglabs28.btradiusdetector.ui.theme.SonarGreen

/**
 * Pure, testable signal helpers. No Compose state — safe for unit tests.
 */
object SignalUtils {

    fun proximityLabel(rssi: Int): String = when (getDistanceCategory(rssi)) {
        DistanceCategory.HOT -> Strings.proximityStrong
        DistanceCategory.WARM -> Strings.proximityMedium
        DistanceCategory.COLD -> Strings.proximityWeak
        DistanceCategory.UNKNOWN -> Strings.searching
    }

    fun proximityColorName(rssi: Int): DistanceCategory = getDistanceCategory(rssi)

    fun rssiToProgress(rssi: Int): Float =
        ((rssi - Constants.RSSI_FLOOR).coerceIn(0, Constants.RSSI_RANGE_SPAN) / Constants.RSSI_RANGE_SPAN.toFloat())

    fun filledSegments(rssi: Int, segments: Int = Constants.SIGNAL_SEGMENTS): Int =
        (rssiToProgress(rssi) * segments).toInt().coerceIn(0, segments)

    fun dotScale(rssi: Int): Float =
        Constants.DOT_SCALE_BASE + rssiToProgress(rssi) * Constants.DOT_SCALE_RANGE

    fun formattedDbm(rssi: Int): String = Strings.dbmLabel(rssi)

    fun formattedBestSignal(peakHeading: Float): String =
        Strings.bestSignalLabel(peakHeading.toInt())

    fun formattedHeading(heading: Float): String =
        Strings.headingLabel(heading.toInt(), AppUtils.getCardinalDirection(heading))

    /**
     * Buzz action label for a Bluetooth major device class, or null when the
     * class has no buzzable companion surface yet (watches today; headphones,
     * earbuds and speakers hide the button instead of showing a dead one).
     */
    fun buzzLabelFor(majorClass: Int): String? = when (majorClass) {
        BluetoothClass.Device.Major.WEARABLE -> Strings.buzzMyWatch
        else -> null
    }

    /**
     * Signed turn from [heading] toward [peakHeading], in (-180, 180].
     * Positive = turn right, negative = turn left.
     */
    fun relativeTurn(peakHeading: Float, heading: Float): Float =
        ((peakHeading - heading + 540f) % 360f) - 180f

    fun turnGuidance(peakHeading: Float, heading: Float): String {
        val relative = relativeTurn(peakHeading, heading)
        return when {
            kotlin.math.abs(relative) <= Constants.GUIDANCE_DEAD_ZONE_DEG -> Strings.facingBestSignal
            relative > 0 -> Strings.turnRight(relative.toInt())
            else -> Strings.turnLeft(-relative.toInt())
        }
    }

    @Composable
    fun proximityColor(rssi: Int): Color = when (getDistanceCategory(rssi)) {
        DistanceCategory.HOT -> ProximityHot
        DistanceCategory.WARM -> ProximityWarm
        DistanceCategory.COLD -> ProximityCold
        DistanceCategory.UNKNOWN -> androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
    }

    fun isValidSignal(rssi: Int): Boolean = rssi > Constants.RSSI_FLOOR

    /** Green is the brand accent for valid-signal chrome; kept here to avoid hardcoding in UI. */
    fun brandAccent(): Color = SonarGreen
}
