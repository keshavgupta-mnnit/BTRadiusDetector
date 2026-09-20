package com.kglabs28.btradiusdetector.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceCategoryTest {

    @Test
    fun `test getDistanceCategory with various RSSI values`() {
        assertEquals(DistanceCategory.HOT, getDistanceCategory(-50))
        assertEquals(DistanceCategory.HOT, getDistanceCategory(-60))
        assertEquals(DistanceCategory.WARM, getDistanceCategory(-61))
        assertEquals(DistanceCategory.WARM, getDistanceCategory(-80))
        assertEquals(DistanceCategory.COLD, getDistanceCategory(-81))
        assertEquals(DistanceCategory.COLD, getDistanceCategory(-99))
        assertEquals(DistanceCategory.UNKNOWN, getDistanceCategory(-100))
        assertEquals(DistanceCategory.UNKNOWN, getDistanceCategory(-110))
    }
}
