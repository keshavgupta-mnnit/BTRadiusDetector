package com.kglabs28.btradiusdetector.domain.model

/** How a disconnect alert should sound. Stored by name in the flags table. */
enum class AlertSoundMode {
    OFF,
    ONCE,
    CONTINUOUS;

    companion object {
        fun fromName(name: String?): AlertSoundMode =
            values().firstOrNull { it.name == name } ?: ONCE
    }
}
