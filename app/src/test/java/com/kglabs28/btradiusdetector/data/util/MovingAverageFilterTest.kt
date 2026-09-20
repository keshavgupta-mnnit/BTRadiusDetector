package com.kglabs28.btradiusdetector.data.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MovingAverageFilterTest {

    @Test
    fun `test moving average calculation`() {
        val filter = MovingAverageFilter(3)
        
        assertEquals(10.0, filter.add(10), 0.01)
        assertEquals(15.0, filter.add(20), 0.01)
        assertEquals(20.0, filter.add(30), 0.01)
        
        // Window is full (10, 20, 30), next value 40 replaces 10
        assertEquals(30.0, filter.add(40), 0.01) // (20 + 30 + 40) / 3 = 30
    }

    @Test
    fun `test clear filter`() {
        val filter = MovingAverageFilter(3)
        filter.add(10)
        filter.add(20)
        filter.clear()
        
        assertEquals(15.0, filter.add(15), 0.01)
    }
}
