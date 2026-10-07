package com.kglabs28.btradiusdetector.domain.model

/** Alert repetition. Stored by name in the flags table. */
enum class AlertRepeatMode {
    ONCE,
    CONTINUOUS;

    companion object {
        fun fromName(name: String?): AlertRepeatMode =
            values().firstOrNull { it.name == name } ?: ONCE
    }
}
