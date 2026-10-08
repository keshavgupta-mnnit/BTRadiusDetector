package com.kglabs28.btradiusdetector.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kglabs28.btradiusdetector.domain.model.SignalPoint
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.ui.theme.ContentWhite
import com.kglabs28.btradiusdetector.ui.theme.SonarCyan
import com.kglabs28.btradiusdetector.ui.theme.SonarGreen
import com.kglabs28.btradiusdetector.utils.Constants
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.domain.signal.SignalEngine
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

/**
 * The full radar block in one reusable piece: rings + compass numbers +
 * pulsing dot + best-signal marker + turn guidance. Drop it on any screen
 * that has heading + signal data — no radar internals leak out.
 */
@Composable
fun RadarTracker(
    heading: Float,
    peakHeading: Float,
    peakRssi: Int,
    rssi: Int,
    history: List<SignalPoint> = emptyList(),
    modifier: Modifier = Modifier
) {
    val animatedHeading by animateFloatAsState(
        targetValue = heading,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "HeadingAnimation"
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.fillMaxWidth().height(Dimens.radarBoxHeight.scaled()),
            contentAlignment = Alignment.Center
        ) {
            RadarView(
                peakHeading = peakHeading,
                peakRssi = peakRssi,
                currentHeading = heading,
                history = history
            )
            CompassRing(heading = animatedHeading)
            PulsingDot(scale = SignalEngine.dotScale(rssi))
            if (peakRssi > Constants.RSSI_FLOOR) {
                Text(
                    text = SignalEngine.formattedBestSignal(peakHeading),
                    color = SonarGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = Dimens.textBestSignal,
                    textAlign = TextAlign.End,
                    lineHeight = Dimens.lineHeightBestSignal,
                    modifier = Modifier.align(Alignment.TopEnd)
                        .padding(top = Dimens.spacingSm.scaled(), end = Dimens.spacingXs.scaled())
                )
            }
        }
        if (peakRssi > Constants.RSSI_FLOOR) {
            Text(
                text = SignalEngine.guidance(peakHeading, heading),
                color = SonarGreen,
                fontWeight = FontWeight.SemiBold,
                fontSize = Dimens.textCaption,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = Dimens.spacingXs.scaled())
            )
        }
    }
}

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

        // Best-direction wedge: the single direction cue on this radar.
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

        history.forEachIndexed { index, point ->
            val alpha = (1f - (index / Constants.SIGNAL_HISTORY_MAX.toFloat()).coerceIn(0f, 1f)) * Dimens.alphaHistory
            val markerRadius = maxRadius * SignalEngine.rssiToProgress(point.rssi)
            drawCircle(
                color = SonarGreen.copy(alpha = alpha),
                radius = 3.dp.toPx(),
                center = SignalEngine.pointAt(
                    center, markerRadius,
                    SignalEngine.relativeBearing(point.heading, currentHeading)
                ),
                style = Fill
            )
        }

        if (peakRssi > Constants.RSSI_FLOOR) {
            val bearing = SignalEngine.relativeBearing(peakHeading, currentHeading)
            val peak = SignalEngine.pointAt(center, maxRadius * 0.92f, bearing)
            drawCircle(color = SonarGreen.copy(alpha = Dimens.alphaPeakHalo), radius = 6.dp.toPx(), center = peak)
            drawCircle(color = SonarGreen, radius = 3.5.dp.toPx(), center = peak)

            // Bearing needle on the outer rim: unambiguous "walk this way" pointer.
            val angle = SignalEngine.angleRad(bearing)
            val dir = Offset(kotlin.math.cos(angle).toFloat(), kotlin.math.sin(angle).toFloat())
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
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2 - 12.dp.toPx()
            for (deg in 0 until 360 step Constants.COMPASS_TICK_STEP_DEG) {
                val isMajor = deg % 90 == 0
                val outer = radius
                val inner = radius - (if (isMajor) 8.dp else 4.dp).toPx()
                drawLine(
                    color = SonarCyan.copy(alpha = if (isMajor) 0.8f else 0.3f),
                    start = SignalEngine.pointAt(center, inner, deg - heading),
                    end = SignalEngine.pointAt(center, outer, deg - heading),
                    strokeWidth = (if (isMajor) 2.dp else 1.dp).toPx()
                )
            }
        }
        // Labels ride the rotating ring but stay upright: the ring positions
        // them, the counter-rotation keeps glyphs readable at every heading.
        for (deg in 0 until 360 step Constants.COMPASS_LABEL_STEP_DEG) {
            val rotation = deg - heading
            Box(
                modifier = Modifier.fillMaxSize().rotate(rotation),
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
                    Text(
                        text = cardinal, color = ContentWhite, fontWeight = FontWeight.Bold, fontSize = Dimens.textRadarLabel,
                        modifier = Modifier.rotate(-rotation)
                    )
                } else {
                    Text(
                        text = "$deg",
                        color = ContentWhite.copy(alpha = Dimens.alphaSubtleText),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = Dimens.textTiny,
                        modifier = Modifier.rotate(-rotation)
                    )
                }
            }
        }
    }
}

/**
 * Center bearing arrow: replaces the plain dot once a peak exists, so the
 * middle of the radar points where the needle points. Falls back to the
 * pulsing dot while searching.
 */
@Composable
fun BearingArrow(
    peakHeading: Float,
    currentHeading: Float,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArrowPulse")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(Constants.DOT_PULSE_DURATION_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ArrowGlow"
    )
    Canvas(modifier = modifier.size(Dimens.pulsingGlowSize.scaled())) {
        val center = Offset(size.width / 2, size.height / 2)
        val base = (size.width / 2) * scale
        drawCircle(color = SonarGreen.copy(alpha = Dimens.alphaGlow), radius = base * glowScale)
        rotate(SignalEngine.relativeBearing(peakHeading, currentHeading), center) {
            val tip = center + Offset(0f, -base)
            val half = base * 0.45f
            drawPath(
                Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(center.x + half, center.y + half)
                    lineTo(center.x - half, center.y + half)
                    close()
                },
                color = SonarGreen
            )
        }
        drawCircle(color = ContentWhite.copy(alpha = Dimens.alphaDotCenter), radius = base * 0.3f)
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
        RadarTracker(heading = 42f, peakHeading = 42f, peakRssi = -52, rssi = -52)
    }
}
