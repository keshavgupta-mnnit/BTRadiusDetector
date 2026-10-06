package com.kglabs28.btradiusdetector.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kglabs28.btradiusdetector.ui.MainViewModel
import com.kglabs28.btradiusdetector.ui.theme.SonarCyan
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.ui.theme.SonarGreen
import com.kglabs28.btradiusdetector.utils.DistanceCategory
import com.kglabs28.btradiusdetector.utils.getDistanceCategory
import kotlin.math.roundToInt

private fun proximityLabel(rssi: Int): String {
    return when (getDistanceCategory(rssi)) {
        DistanceCategory.HOT -> "Strong"
        DistanceCategory.WARM -> "Medium"
        DistanceCategory.COLD -> "Weak"
        DistanceCategory.UNKNOWN -> "Searching..."
    }
}

private fun proximityColor(rssi: Int): Color {
    return when (getDistanceCategory(rssi)) {
        DistanceCategory.HOT -> SonarGreen
        DistanceCategory.WARM -> SonarCyan
        DistanceCategory.COLD -> Color(0xFFFFBB33)
        DistanceCategory.UNKNOWN -> Color.Gray
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    deviceId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val heading by viewModel.headingFlow.collectAsStateWithLifecycle(initialValue = 0f)
    val rssiFlow = remember(deviceId) { viewModel.getRssiFlow(deviceId) }
    val rssi by rssiFlow.collectAsStateWithLifecycle(initialValue = -100)

    val peakRssi by viewModel.peakRssi.collectAsStateWithLifecycle()
    val peakHeading by viewModel.peakHeading.collectAsStateWithLifecycle()
    val signalHistory by viewModel.signalHistory.collectAsStateWithLifecycle()

    val bondedDevices by viewModel.bondedDevices.collectAsStateWithLifecycle()
    val device = remember(bondedDevices, deviceId) {
        bondedDevices.find { it.address == deviceId }
    }
    val deviceName = device?.name ?: "Unknown Device"
    val isConnected = device?.isConnected ?: true

    var showHelp by remember { mutableStateOf(false) }

    LaunchedEffect(rssi, heading) {
        viewModel.updateSignalData(rssi, heading)
    }

    val animatedHeading by animateFloatAsState(
        targetValue = heading,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "HeadingAnimation"
    )

    val normalizedRssi = ((rssi + 100).coerceIn(0, 70) / 70f)
    val dotScale by animateFloatAsState(
        targetValue = 0.5f + normalizedRssi * 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "RssiPulse"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Finding: $deviceName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1
                        )
                        Text(
                            if (isConnected) "Connected" else "Disconnected",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 12.sp,
                            color = if (isConnected) SonarGreen else MaterialTheme.colorScheme.error
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetPeak()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(Icons.Rounded.Info, contentDescription = "How to find")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Radar with Best-signal label
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(330.dp),
                contentAlignment = Alignment.Center
            ) {
                RadarView(
                    peakHeading = peakHeading,
                    peakRssi = peakRssi,
                    currentHeading = heading,
                    history = signalHistory
                )
                CompassRing(
                    heading = animatedHeading,
                    peakHeading = peakHeading,
                    peakRssi = peakRssi
                )
                PulsingDot(scale = dotScale)
                if (peakRssi > -100) {
                    Text(
                        text = "Best signal\n(${peakHeading.roundToInt()}°)",
                        color = SonarGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        textAlign = TextAlign.End,
                        lineHeight = 13.sp,
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Signal strength card (matches design)
            SignalStrengthCard(rssi = rssi)

            Spacer(modifier = Modifier.height(12.dp))

            // Heading card (matches design)
            HeadingCardNew(heading = heading)

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "Watch buzz requires companion app", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SonarGreen.copy(alpha = 0.8f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SonarGreen)
            ) {
                Icon(Icons.Rounded.Watch, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buzz my watch", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showHelp) {
        HowToFindDialog(onDismiss = { showHelp = false })
    }
}

@Composable
fun SignalStrengthCard(rssi: Int) {
    val label = proximityLabel(rssi)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Wifi,
                contentDescription = null,
                tint = SonarGreen,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Signal strength",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                "$rssi dBm",
                style = MaterialTheme.typography.labelMedium,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        SegmentedSignalBar(
            rssi = rssi,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
        )
    }
}

@Composable
fun SegmentedSignalBar(rssi: Int, modifier: Modifier = Modifier) {
    val progress = ((rssi + 100).coerceIn(0, 70) / 70f)
    val segments = 20
    val filled = (progress * segments).roundToInt()
    val barColor = proximityColor(rssi)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(segments) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (i < filled) 14.dp else 8.dp)
                    .align(Alignment.CenterVertically)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (i < filled) barColor else barColor.copy(alpha = 0.18f)
                    )
            )
        }
    }
}

@Composable
fun HeadingCardNew(heading: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Explore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Current heading",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    "${heading.roundToInt()}° ${com.kglabs28.btradiusdetector.utils.AppUtils.getCardinalDirection(heading)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun RadarView(
    peakHeading: Float,
    peakRssi: Int,
    currentHeading: Float,
    history: List<com.kglabs28.btradiusdetector.ui.SignalPoint> = emptyList()
) {
    val secondaryColor = SonarGreen
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepRotation"
    )

    Canvas(modifier = Modifier.size(300.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.width / 2 - 8.dp.toPx()

        // Rings
        for (i in 1..4) {
            drawCircle(
                color = if (i == 4) SonarCyan.copy(alpha = 0.6f) else SonarGreen.copy(alpha = 0.22f),
                radius = maxRadius * (i / 4f),
                style = Stroke(width = (if (i == 4) 1.8.dp else 1.dp).toPx())
            )
        }
        // Cross lines
        drawLine(
            color = SonarGreen.copy(alpha = 0.15f),
            start = Offset(center.x - maxRadius, center.y),
            end = Offset(center.x + maxRadius, center.y),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = SonarGreen.copy(alpha = 0.15f),
            start = Offset(center.x, center.y - maxRadius),
            end = Offset(center.x, center.y + maxRadius),
            strokeWidth = 1.dp.toPx()
        )

        // Best-direction wedge (matches design green sector)
        if (peakRssi > -100) {
            val relative = peakHeading - currentHeading
            rotate(relative - 17.5f, center) {
                drawArc(
                    color = SonarGreen.copy(alpha = 0.28f),
                    startAngle = -90f,
                    sweepAngle = 35f,
                    useCenter = true,
                    topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                    size = Size(maxRadius * 2, maxRadius * 2)
                )
            }
        }

        // Faint rotating sweep for liveliness
        rotate(sweepRotation, center) {
            drawArc(
                brush = Brush.sweepGradient(
                    0.8f to Color.Transparent,
                    1.0f to SonarGreen.copy(alpha = 0.25f),
                    center = center
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = true,
                size = size,
                topLeft = Offset.Zero
            )
        }

        // History dots
        history.forEachIndexed { index, point ->
            val alpha = (1f - (index / 20f).coerceIn(0f, 1f)) * 0.45f
            val angleRad = Math.toRadians((point.heading - currentHeading - 90).toDouble())
            val markerRadius = maxRadius * ((point.rssi + 100).coerceIn(0, 70) / 70f)
            val markerX = center.x + markerRadius * kotlin.math.cos(angleRad).toFloat()
            val markerY = center.y + markerRadius * kotlin.math.sin(angleRad).toFloat()
            drawCircle(
                color = SonarGreen.copy(alpha = alpha),
                radius = 3.dp.toPx(),
                center = Offset(markerX, markerY),
                style = Fill
            )
        }

        // Peak dots on outer area (green, like design)
        if (peakRssi > -100) {
            val angleRad = Math.toRadians((peakHeading - currentHeading - 90).toDouble())
            val pr = (maxRadius * 0.92f)
            val px = center.x + pr * kotlin.math.cos(angleRad).toFloat()
            val py = center.y + pr * kotlin.math.sin(angleRad).toFloat()
            drawCircle(color = SonarGreen.copy(alpha = 0.5f), radius = 6.dp.toPx(), center = Offset(px, py))
            drawCircle(color = SonarGreen, radius = 3.5.dp.toPx(), center = Offset(px, py))
        }
    }
}

@Composable
fun CompassRing(heading: Float, peakHeading: Float, peakRssi: Int) {
    Box(modifier = Modifier.size(330.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.width / 2 - 12.dp.toPx()
            // tick marks
            for (deg in 0 until 360 step 6) {
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

        val markers = listOf("N", "E", "S", "W")
        markers.forEachIndexed { index, label ->
            val angle = (index * 90f) - heading
            Box(
                modifier = Modifier.fillMaxSize().rotate(angle),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(top = 0.dp)
                )
            }
        }
    }
}

@Composable
fun PulsingDot(scale: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "DotPulse")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowScale"
    )

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(60.dp)) {
            drawCircle(
                color = SonarGreen.copy(alpha = 0.22f),
                radius = (size.width / 2) * scale * glowScale
            )
        }
        Canvas(modifier = Modifier.size(30.dp)) {
            drawCircle(color = SonarGreen, radius = (size.width / 2) * scale)
            drawCircle(color = Color.White.copy(alpha = 0.85f), radius = (size.width / 4) * scale)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun TrackingScreenPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(330.dp), contentAlignment = Alignment.Center) {
                        RadarView(peakHeading = 42f, peakRssi = -52, currentHeading = 42f)
                        CompassRing(heading = 42f, peakHeading = 42f, peakRssi = -52)
                        PulsingDot(scale = 0.85f)
                        Text(
                            "Best signal\n(42°)",
                            color = SonarGreen, fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
                            textAlign = TextAlign.End, lineHeight = 13.sp,
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SignalStrengthCard(rssi = -52)
                    Spacer(modifier = Modifier.height(12.dp))
                    HeadingCardNew(heading = 42f)
                }
            }
        }
    }
}
