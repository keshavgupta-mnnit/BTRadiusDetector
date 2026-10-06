package com.kglabs28.btradiusdetector.ui.theme

import androidx.compose.ui.graphics.Color

// Dark Navy Palette
val DarkNavy = Color(0xFF0A0E1A)
val DeepNavy = Color(0xFF12182B)
val LightNavy = Color(0xFF1C2541)

// Sonar / Expressive Palette
val SonarGreen = Color(0xFF00FF9D)
val SonarCyan = Color(0xFF00E5FF)
val SonarBlue = Color(0xFF2979FF)

// M3 Color Tokens
val PrimaryDark = SonarGreen
val OnPrimaryDark = Color(0xFF003920)
val PrimaryContainerDark = Color(0xFF005230)
val OnPrimaryContainerDark = Color(0xFF7CFFB7)

val SecondaryDark = SonarCyan
val OnSecondaryDark = Color(0xFF00363D)
val SecondaryContainerDark = Color(0xFF004F58)
val OnSecondaryContainerDark = Color(0xFF97F0FF)

val BackgroundDark = DarkNavy
val OnBackgroundDark = Color(0xFFE2E2E6)
val SurfaceDark = DeepNavy
val OnSurfaceDark = Color(0xFFE2E2E6)

// Shared semantic tokens — use these instead of inline Color(...) in UI files
val ContentWhite = Color(0xFFFFFFFF)
val ScrimBlack = Color(0xFF000000)
val WarningAmber = Color(0xFFFFBB33)
val ProximityHot = SonarGreen
val ProximityWarm = SonarCyan
val ProximityCold = WarningAmber

// Notification accents (framework NotificationCompat.setColor takes the
// ARGB int via .toArgb(), so these stay in the design system, not in res).
val NotificationAccentDisconnect = Color(0xFFFF5252)
val NotificationAccentReconnect = SonarGreen

// Fallback Light Palette (if needed, though dark navy is the focus)
val PrimaryLight = Color(0xFF006D41)
val OnPrimaryLight = Color.White
val BackgroundLight = Color(0xFFFBFDF8)
val OnBackgroundLight = Color(0xFF191C1A)
