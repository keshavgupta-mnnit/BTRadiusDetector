package com.kglabs28.btradiusdetector.utils

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Dimens {
    // Spacing
    val spacingXs = 4.dp
    val spacingSm = 8.dp
    val spacingMd = 16.dp
    val spacingLg = 24.dp
    val spacingXl = 32.dp

    // Corner radius
    val cornerRadiusCard = 16.dp
    val cornerRadiusButton = 12.dp

    // Icon circle backgrounds
    val iconCircleSize = 48.dp
    val iconCircleSizeSmall = 40.dp

    // Icon glyph sizes
    val iconSizeSmall = 20.dp
    val iconSizeMedium = 32.dp
    val iconSizeLarge = 64.dp
    val iconArrowSize = 16.dp

    // Elevation
    val elevationCard = 2.dp
    val elevationSurfaceLow = 1.dp

    // Borders
    val borderWidthThin = 1.dp

    // Radar / compass (used once we build TrackingScreen)
    val radarSize = 360.dp
    val compassRingSize = 400.dp

    // Fractions — proportional, never passed through scaled()
    const val cardWidthFraction = 0.85f

    // Alpha
    const val alphaDisabledContent = 0.5f
    const val alphaSubtleText = 0.6f
    const val alphaMuted = 0.4f
    const val alphaBorderSubtle = 0.3f
    const val alphaBackgroundTint = 0.05f

    // Text
    val letterSpacingButton = 1.sp
}