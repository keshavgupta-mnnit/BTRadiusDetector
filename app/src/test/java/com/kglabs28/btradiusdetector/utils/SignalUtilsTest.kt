package com.kglabs28.btradiusdetector.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalUtilsTest {

    @Test
    fun `proximity labels map to categories`() {
        assertEquals(Strings.proximityStrong, SignalUtils.proximityLabel(-50))
        assertEquals(Strings.proximityMedium, SignalUtils.proximityLabel(-70))
        assertEquals(Strings.proximityWeak, SignalUtils.proximityLabel(-90))
        assertEquals(Strings.searching, SignalUtils.proximityLabel(-100))
    }

    @Test
    fun `rssi progress is clamped 0 to 1`() {
        assertEquals(1f, SignalUtils.rssiToProgress(-30))
        assertEquals(0f, SignalUtils.rssiToProgress(-100))
        assertTrue(SignalUtils.rssiToProgress(-60) > 0f)
    }

    @Test
    fun `filled segments scale with rssi`() {
        assertEquals(20, SignalUtils.filledSegments(-30, 20))
        assertEquals(0, SignalUtils.filledSegments(-100, 20))
        assertTrue(SignalUtils.filledSegments(-52, 20) > SignalUtils.filledSegments(-80, 20))
    }

    @Test
    fun `valid signal excludes floor`() {
        assertTrue(SignalUtils.isValidSignal(-50))
        assertFalse(SignalUtils.isValidSignal(-100))
    }

    @Test
    fun `formatters use centralized strings`() {
        assertEquals("-52 dBm", SignalUtils.formattedDbm(-52))
        assertEquals("42° N", SignalUtils.formattedHeading(42f))
        assertEquals("45° NE", SignalUtils.formattedHeading(45f))
    }

    @Test
    fun `relative turn wraps to signed shortest path`() {
        assertEquals(20f, SignalUtils.relativeTurn(60f, 40f))
        assertEquals(-20f, SignalUtils.relativeTurn(40f, 60f))
        assertEquals(20f, SignalUtils.relativeTurn(10f, 350f))
        assertEquals(-20f, SignalUtils.relativeTurn(350f, 10f))
    }

    @Test
    fun `turn guidance names direction or arrival`() {
        assertEquals(Strings.facingBestSignal, SignalUtils.turnGuidance(42f, 45f))
        assertEquals(Strings.turnRight(30), SignalUtils.turnGuidance(70f, 40f))
        assertEquals(Strings.turnLeft(30), SignalUtils.turnGuidance(40f, 70f))
    }
}
