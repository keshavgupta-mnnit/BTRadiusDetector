package com.kglabs28.btradiusdetector.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface NavRoute : NavKey {
    @Serializable
    data object Onboarding : NavRoute

    @Serializable
    data object Scan : NavRoute

    @Serializable
    data object Settings : NavRoute

    @Serializable
    data class Details(val deviceId: String) : NavRoute
}
