package com.kglabs28.btradiusdetector.domain.model

/** One posted alert for the notification history. */
data class AlertActivity(
    val address: String,
    val deviceName: String,
    val event: String,
    val posted: Boolean,
    val reason: String,
    val majorClass: Int = 0,
    val minorClass: Int = 0,
    val atMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val REASON_NOT_REGISTERED = "not_registered"
        const val REASON_LINK_ALIVE = "link_alive"
        const val REASON_DUPLICATE = "duplicate"
    }
}
