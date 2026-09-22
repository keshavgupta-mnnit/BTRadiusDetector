package com.kglabs28.btradiusdetector.utils

import android.content.Context
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Resolves a single screen-size multiplier once per process lifetime and
 * caches it. Every dimension in the app is multiplied through this instead
 * of branching per-call, so reads stay a flat float multiply with no
 * recomputation cost — safe to call from continuously-redrawing composables
 * like the radar/compass views.
 */
object ScreenScale {

    @Volatile
    private var multiplier: Float? = null

    fun init(context: Context) {
        if (multiplier != null) return
        val swDp = context.resources.configuration.smallestScreenWidthDp
        multiplier = resolveMultiplier(swDp)
    }

    fun scale(): Float = multiplier ?: 1f

    private fun resolveMultiplier(swDp: Int): Float = when {
        swDp >= 720 -> 1.5f
        swDp >= 600 -> 1.35f
        swDp >= 480 -> 1.2f
        swDp >= 400 -> 1.1f
        swDp >= 360 -> 1.0f
        else -> 0.9f
    }
}

fun Dp.scaled(): Dp = this * ScreenScale.scale()
fun Int.scaledDp(): Dp = this.dp * ScreenScale.scale()