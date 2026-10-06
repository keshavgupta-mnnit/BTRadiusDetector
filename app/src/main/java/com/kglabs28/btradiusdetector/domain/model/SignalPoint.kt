package com.kglabs28.btradiusdetector.domain.model

/** One smoothed RSSI sample tagged with the compass heading it was read at. */
data class SignalPoint(val rssi: Int, val heading: Float, val timestamp: Long)
