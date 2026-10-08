package com.kglabs28.btradiusdetector.domain.signal

import android.bluetooth.BluetoothClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.kglabs28.btradiusdetector.ui.theme.ProximityCold
import com.kglabs28.btradiusdetector.ui.theme.ProximityHot
import com.kglabs28.btradiusdetector.ui.theme.ProximityWarm
import com.kglabs28.btradiusdetector.utils.AppUtils
import com.kglabs28.btradiusdetector.utils.Constants
import com.kglabs28.btradiusdetector.utils.DistanceCategory
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.getDistanceCategory

/**
 * THE single home for every signal/direction decision in the app:
 * strength mapping, colors, progress, bearings, turn guidance, formatting.
 * Pure functions only (one @Composable color reader) — fully unit-testable,
 * no Android framework beyond Compose primitives. UI layers draw; they never
 * compute. If bearings ever disagree on screen again, the bug is here.
 *
 * Angle convention (shared by radar, needle, arrow and labels): degrees
 * clockwise from north, matching the compass sensor. On-canvas, 0° points
 * up, so rendering subtracts 90° — see [angleRad]. All bearings below follow
 * this convention; do not invent another one.
 */
object SignalEngine {

    // ---- validity ----

    fun isValidSignal(rssi: Int): Boolean = rssi > Constants.RSSI_FLOOR

    fun proximityCategory(rssi: Int): DistanceCategory = getDistanceCategory(rssi)

    // ---- strength presentation ----

    fun proximityLabel(rssi: Int): String = when (getDistanceCategory(rssi)) {
        DistanceCategory.HOT -> Strings.proximityStrong
        DistanceCategory.WARM -> Strings.proximityMedium
        DistanceCategory.COLD -> Strings.proximityWeak
        DistanceCategory.UNKNOWN -> Strings.searching
    }

    @Composable
    fun proximityColor(rssi: Int): Color = when (getDistanceCategory(rssi)) {
        DistanceCategory.HOT -> ProximityHot
        DistanceCategory.WARM -> ProximityWarm
        DistanceCategory.COLD -> ProximityCold
        DistanceCategory.UNKNOWN -> androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
    }

    /** 0 (floor) .. 1 (hot or better), clamped. */
    fun rssiToProgress(rssi: Int): Float =
        ((rssi - Constants.RSSI_FLOOR).coerceIn(0, Constants.RSSI_RANGE_SPAN) / Constants.RSSI_RANGE_SPAN.toFloat())

    fun filledSegments(rssi: Int, segments: Int = Constants.SIGNAL_SEGMENTS): Int =
        (rssiToProgress(rssi) * segments).toInt().coerceIn(0, segments)

    fun dotScale(rssi: Int): Float =
        Constants.DOT_SCALE_BASE + rssiToProgress(rssi) * Constants.DOT_SCALE_RANGE

    // ---- bearings ----

    fun cardinal(heading: Float): String = AppUtils.getCardinalDirection(heading)

    /**
     * Signed shortest turn from [heading] toward [peakHeading], in (-180, 180].
     * Positive = turn right, negative = turn left.
     */
    fun relativeTurn(peakHeading: Float, heading: Float): Float =
        ((peakHeading - heading + 540f) % 360f) - 180f

    /** Unsigned bearing of the peak relative to facing, in [0, 360). */
    fun relativeBearing(peakHeading: Float, heading: Float): Float =
        ((peakHeading - heading) % 360f + 360f) % 360f

    /** Canvas angle (radians) for a compass bearing under the shared convention. */
    fun angleRad(bearingDeg: Float): Double =
        Math.toRadians((bearingDeg - 90).toDouble())

    /** Point on a circle: the one geometry helper every marker uses. */
    fun pointAt(center: Offset, radius: Float, bearingDeg: Float): Offset {
        val angle = angleRad(bearingDeg)
        return Offset(
            center.x + radius * kotlin.math.cos(angle).toFloat(),
            center.y + radius * kotlin.math.sin(angle).toFloat()
        )
    }

    /**
     * One-line turn instruction. Near-aligned faces the target; near-opposite
     * tells the user to turn around instead of reciting ~180° of turning.
     */
    fun guidance(peakHeading: Float, heading: Float): String {
        val relative = relativeTurn(peakHeading, heading)
        return when {
            kotlin.math.abs(relative) <= Constants.GUIDANCE_DEAD_ZONE_DEG -> Strings.facingBestSignal
            kotlin.math.abs(relative) >= Constants.TURN_AROUND_DEG -> Strings.turnAround
            relative > 0 -> Strings.turnRight(relative.toInt())
            else -> Strings.turnLeft(-relative.toInt())
        }
    }

    // ---- formatting ----

    fun formattedDbm(rssi: Int): String = Strings.dbmLabel(rssi)

    fun formattedBestSignal(peakHeading: Float): String =
        Strings.bestSignalLabel(peakHeading.toInt())

    fun formattedHeading(heading: Float): String =
        Strings.headingLabel(heading.toInt(), cardinal(heading))

    /**
     * Buzz action label for a Bluetooth major device class, or null when the
     * class has no buzzable companion surface yet (watches today; headphones,
     * earbuds and speakers hide the button instead of showing a dead one).
     */
    fun buzzLabelFor(majorClass: Int): String? = when (majorClass) {
        BluetoothClass.Device.Major.WEARABLE -> Strings.buzzMyWatch
        else -> null
    }
}
