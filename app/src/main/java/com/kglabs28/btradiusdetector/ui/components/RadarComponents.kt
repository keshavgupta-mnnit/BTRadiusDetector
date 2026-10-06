package com.kglabs28.btradiusdetector.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kglabs28.btradiusdetector.ui.SignalPoint
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.ui.theme.ContentWhite
import com.kglabs28.btradiusdetector.ui.theme.SonarCyan
import com.kglabs28.btradiusdetector.ui.theme.SonarGreen
import com.kglabs28.btradiusdetector.utils.Constants
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.scaled

/**
 * Radar rings + best-direction wedge + history dots. Stateless.
 */
@Composable
fun RadarView(
    peakHeading: Float,
    peakRssi: Int,
    currentHeading: Float,
    history: List<SignalPoint> = emptyList(),
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(Constants.RADAR_SWEEP_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepRotation"
    )

    Canvas(modifier = modifier.size(Dimens.radarSize.scaled())) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.width / 2 - Dimens.spacingSm.scaled().toPx()

        for (i in 1..Constants.RADAR_RINGS) {
            drawCircle(
                color = if (i == Constants.RADAR_RINGS) SonarCyan.copy(alpha = 0.6f)
                else SonarGreen.copy(alpha = Dimens.alphaRingFaint),
                radius = maxRadius * (i / Constants.RADAR_RINGS.toFloat()),
                style = Stroke(width = Dimens.borderWidthThin.scaled().toPx())
            )
        }
        drawLine(
            color = SonarGreen.copy(alpha = Dimens.alphaCrosshair),
            start = Offset(center.x - maxRadius, center.y),
            end = Offset(center.x + maxRadius, center.y),
            strokeWidth = Dimens.borderWidthThin.scaled().toPx()
        )
        drawLine(
            color = SonarGreen.copy(alpha = Dimens.alphaCrosshair),
            start = Offset(center.x, center.y - maxRadius),
            end = Offset(center.x, center.y + maxRadius),
            strokeWidth = Dimens.borderWidthThin.scaled().toPx()
        )

        if (peakRssi > Constants.RSSI_FLOOR) {
            val relative = peakHeading - currentHeading
            rotate(relative - Constants.BEST_WEDGE_OFFSET_DEG, center) {
                drawArc(
                    color = SonarGreen.copy(alpha = Dimens.alphaWedge),
                    startAngle = -90f,
                    sweepAngle = Constants.BEST_WEDGE_SWEEP_DEG,
                    useCenter = true,
                    topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                    size = Size(maxRadius * 2, maxRadius * 2)
                )
            }
        }

        rotate(sweepRotation, center) {
            drawArc(
                brush = Brush.sweepGradient(
                    0.8f to Color.Transparent,
                    1.0f to SonarGreen.copy(alpha = Dimens.alphaSweep),
                    center = center
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = true,
                size = size,
                topLeft = Offset.Zero
            )
        }

        history.forEachIndexed { index, point ->
            val alpha = (1f - (index / Constants.SIGNAL_HISTORY_MAX.toFloat()).coerceIn(0f, 1f)) * Dimens.alphaHistory
            val angleRad = Math.toRadians((point.heading - currentHeading - 90).toDouble())
            val markerRadius = maxRadius * ((point.rssi - Constants.RSSI_FLOOR).coerceIn(0, Constants.RSSI_RANGE_SPAN) / Constants.RSSI_RANGE_SPAN.toFloat())
            drawCircle(
                color = SonarGreen.copy(alpha = alpha),
                radius = 3.dp.toPx(),
                center = Offset(
                    center.x + markerRadius * kotlin.math.cos(angleRad).toFloat(),
                    center.y + markerRadius * kotlin.math.sin(angleRad).toFloat()
                ),
                style = Fill
            )
        }

        if (peakRssi > Constants.RSSI_FLOOR) {
            val angleRad = Math.toRadians((peakHeading - currentHeading - 90).toDouble())
            val pr = maxRadius * 0.92f
            val peak = Offset(
                center.x + pr * kotlin.math.cos(angleRad).toFloat(),
                center.y + pr * kotlin.math.sin(angleRad).toFloat()
            )
            drawCircle(color = SonarGreen.copy(alpha = Dimens.alphaPeakHalo), radius = 6.dp.toPx(), center = peak)
            drawCircle(color = SonarGreen, radius = 3.5.dp.toPx(), center = peak)

            // Bearing needle on the outer rim: unambiguous "walk this way" pointer.
            val dir = Offset(kotlin.math.cos(angleRad).toFloat(), kotlin.math.sin(angleRad).toFloat())
            val perp = Offset(-dir.y, dir.x)
            val tip = center + dir * maxRadius
            val baseCenter = center + dir * (maxRadius - 10.dp.toPx())
            val halfWidth = 5.dp.toPx()
            drawPath(
                Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(baseCenter.x + perp.x * halfWidth, baseCenter.y + perp.y * halfWidth)
                    lineTo(baseCenter.x - perp.x * halfWidth, baseCenter.y - perp.y * halfWidth)
                    close()
                },
                color = SonarGreen
            )
        }
    }
}

/**
 * Rotating compass ring: cardinal letters at 0/90/180/270, degree numbers
 * every 30° in between, so the best-signal bearing reads as a number the
 * user can turn toward.
 */
@Composable
fun CompassRing(heading: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(Dimens.compassRingSize.scaled()), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.width / 2 - 12.dp.toPx()
            for (deg in 0 until 360 step Constants.COMPASS_TICK_STEP_DEG) {
                val isMajor = deg % 90 == 0
                val angleRad = Math.toRadians((deg - heading).toDouble() - 90)
                val outer = radius
                val inner = radius - (if (isMajor) 8.dp else 4.dp).toPx()
                drawLine(
                    color = SonarCyan.copy(alpha = if (isMajor) 0.8f else 0.3f),
                    start = Offset(
                        size.width / 2 + inner * kotlin.math.cos(angleRad).toFloat(),
                        size.height / 2 + inner * kotlin.math.sin(angleRad).toFloat()
                    ),
                    end = Offset(
                        size.width / 2 + outer * kotlin.math.cos(angleRad).toFloat(),
                        size.height / 2 + outer * kotlin.math.sin(angleRad).toFloat()
                    ),
                    strokeWidth = (if (isMajor) 2.dp else 1.dp).toPx()
                )
            }
        }
        for (deg in 0 until 360 step Constants.COMPASS_LABEL_STEP_DEG) {
            Box(
                modifier = Modifier.fillMaxSize().rotate(deg - heading),
                contentAlignment = Alignment.TopCenter
            ) {
                val cardinal = when (deg) {
                    0 -> "N"
                    90 -> "E"
                    180 -> "S"
                    270 -> "W"
                    else -> null
                }
                if (cardinal != null) {
                    Text(text = cardinal, color = ContentWhite, fontWeight = FontWeight.Bold, fontSize = Dimens.textRadarLabel)
                } else {
                    Text(
                        text = "$deg",
                        color = ContentWhite.copy(alpha = Dimens.alphaSubtleText),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = Dimens.textTiny
                    )
                }
            }
        }
    }
}

/**
 * Pulsing center dot scaled by signal. Stateless — [scale] in only.
 */
@Composable
fun PulsingDot(scale: Float, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "DotPulse")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(Constants.DOT_PULSE_DURATION_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowScale"
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(Dimens.pulsingGlowSize.scaled())) {
            drawCircle(color = SonarGreen.copy(alpha = Dimens.alphaGlow), radius = (size.width / 2) * scale * glowScale)
        }
        Canvas(modifier = Modifier.size(Dimens.pulsingDotSize.scaled())) {
            drawCircle(color = SonarGreen, radius = (size.width / 2) * scale)
            drawCircle(color = ContentWhite.copy(alpha = Dimens.alphaDotCenter), radius = (size.width / 4) * scale)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RadarPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        Box(contentAlignment = Alignment.Center) {
            RadarView(peakHeading = 42f, peakRssi = -52, currentHeading = 42f)
            CompassRing(heading = 42f)
            PulsingDot(scale = 0.85f)
        }
    }
}
