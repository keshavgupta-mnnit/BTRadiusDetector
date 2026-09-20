package com.kglabs28.btradiusdetector.data.util

import java.util.ArrayDeque

/**
 * A simple moving average filter to smooth RSSI values.
 */
class MovingAverageFilter(private val windowSize: Int) {
    private val window = ArrayDeque<Int>(windowSize)
    private var sum = 0

    @Synchronized
    fun add(value: Int): Double {
        sum += value
        window.addLast(value)
        if (window.size > windowSize) {
            sum -= window.removeFirst()
        }
        return sum.toDouble() / window.size
    }

    @Synchronized
    fun clear() {
        window.clear()
        sum = 0
    }
}
