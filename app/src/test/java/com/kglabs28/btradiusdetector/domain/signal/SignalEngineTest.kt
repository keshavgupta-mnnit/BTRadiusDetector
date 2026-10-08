package com.kglabs28.btradiusdetector.domain.signal

import com.kglabs28.btradiusdetector.utils.Constants
import com.kglabs28.btradiusdetector.utils.DistanceCategory
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.getDistanceCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Single test home for every [SignalEngine] decision. If bearings ever
 * disagree on screen, reproduce it here first.
 */
class SignalEngineTest {

    @Test
    fun `categories follow calibrated thresholds`() {
        assertEquals(DistanceCategory.HOT, getDistanceCategory(-50))
        assertEquals(DistanceCategory.HOT, getDistanceCategory(-70))
        assertEquals(DistanceCategory.WARM, getDistanceCategory(-71))
        assertEquals(DistanceCategory.WARM, getDistanceCategory(-80))
        assertEquals(DistanceCategory.COLD, getDistanceCategory(-81))
        assertEquals(DistanceCategory.COLD, getDistanceCategory(-99))
        assertEquals(DistanceCategory.UNKNOWN, getDistanceCategory(-100))
        assertEquals(DistanceCategory.UNKNOWN, getDistanceCategory(-110))
    }

    @Test
    fun `side by side reads strong`() {
        assertEquals(Strings.proximityStrong, SignalEngine.proximityLabel(-67))
        assertEquals(Strings.proximityStrong, SignalEngine.proximityLabel(-50))
        assertEquals(Strings.proximityMedium, SignalEngine.proximityLabel(-75))
        assertEquals(Strings.proximityWeak, SignalEngine.proximityLabel(-90))
        assertEquals(Strings.searching, SignalEngine.proximityLabel(-100))
    }

    @Test
    fun `rssi progress is clamped 0 to 1`() {
        assertEquals(1f, SignalEngine.rssiToProgress(-30))
        assertEquals(0f, SignalEngine.rssiToProgress(-100))
        assertTrue(SignalEngine.rssiToProgress(-60) > 0f)
    }

    @Test
    fun `filled segments scale with rssi`() {
        assertEquals(20, SignalEngine.filledSegments(-30, 20))
        assertEquals(0, SignalEngine.filledSegments(-100, 20))
        assertTrue(SignalEngine.filledSegments(-52, 20) > SignalEngine.filledSegments(-80, 20))
    }

    @Test
    fun `valid signal excludes floor`() {
        assertTrue(SignalEngine.isValidSignal(-50))
        assertFalse(SignalEngine.isValidSignal(-100))
    }

    @Test
    fun `formatters use centralized strings`() {
        assertEquals("-52 dBm", SignalEngine.formattedDbm(-52))
        assertEquals("42° N", SignalEngine.formattedHeading(42f))
        assertEquals("45° NE", SignalEngine.formattedHeading(45f))
    }

    @Test
    fun `relative turn wraps to signed shortest path`() {
        assertEquals(20f, SignalEngine.relativeTurn(60f, 40f))
        assertEquals(-20f, SignalEngine.relativeTurn(40f, 60f))
        assertEquals(20f, SignalEngine.relativeTurn(10f, 350f))
        assertEquals(-20f, SignalEngine.relativeTurn(350f, 10f))
    }

    @Test
    fun `relative bearing stays in 0 to 360`() {
        assertEquals(20f, SignalEngine.relativeBearing(60f, 40f))
        assertEquals(340f, SignalEngine.relativeBearing(40f, 60f))
        assertEquals(0f, SignalEngine.relativeBearing(42f, 42f))
    }

    @Test
    fun `turn guidance names direction or arrival`() {
        assertEquals(Strings.facingBestSignal, SignalEngine.guidance(42f, 45f))
        assertEquals(Strings.turnRight(30), SignalEngine.guidance(70f, 40f))
        assertEquals(Strings.turnLeft(30), SignalEngine.guidance(40f, 70f))
    }

    @Test
    fun `near opposite bearing says turn around, not degrees`() {
        assertEquals(Strings.turnAround, SignalEngine.guidance(231f, 52f))
        assertEquals(Strings.turnAround, SignalEngine.guidance(52f, 231f))
        assertEquals(Strings.turnAround, SignalEngine.guidance(0f, 180f))
    }

    @Test
    fun `bearing arrow and needle share one bearing`() {
        // The arrow rotates by relativeBearing and the rim needle sits on the
        // same bearing: assert the angle helper agrees with the wedge offset.
        val bearing = SignalEngine.relativeBearing(231f, 52f)
        assertTrue(abs(bearing - 179f) < 0.01f)
        val point = SignalEngine.pointAt(
            androidx.compose.ui.geometry.Offset(100f, 100f), 50f, bearing
        )
        // 179° ≈ pointing down: y grows, x ~ center.
        assertTrue(point.y > 100f)
        assertTrue(abs(point.x - 100f) < 1f)
    }

    @Test
    fun `history constant matches engine span`() {
        assertEquals(70, Constants.RSSI_RANGE_SPAN)
    }
}
