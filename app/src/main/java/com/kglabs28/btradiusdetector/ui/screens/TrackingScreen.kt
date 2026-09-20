package com.kglabs28.btradiusdetector.ui.screens

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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kglabs28.btradiusdetector.ui.MainViewModel
import com.kglabs28.btradiusdetector.ui.theme.SonarCyan
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.ui.theme.SonarGreen
import com.kglabs28.btradiusdetector.util.DistanceCategory
import com.kglabs28.btradiusdetector.util.getDistanceCategory
import kotlin.math.roundToInt

private fun getCardinalDirection(degrees: Float): String {
    val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val normalizedDegrees = (degrees % 360 + 360) % 360
    val index = ((normalizedDegrees + 22.5) / 45).toInt() % 8
    return directions[index]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    deviceId: String, 
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val heading by viewModel.headingFlow.collectAsStateWithLifecycle(initialValue = 0f)
    
    // Explicitly start/stop scanning by collecting from the flow managed by ViewModel
    val rssiFlow = remember(deviceId) { viewModel.getRssiFlow(deviceId) }
    val rssi by rssiFlow.collectAsStateWithLifecycle(initialValue = -100)
    
    val peakRssi by viewModel.peakRssi.collectAsStateWithLifecycle()
    val peakHeading by viewModel.peakHeading.collectAsStateWithLifecycle()
    val signalHistory by viewModel.signalHistory.collectAsStateWithLifecycle()
    
    val bondedDevices by viewModel.bondedDevices.collectAsStateWithLifecycle()
    val deviceName = remember(bondedDevices, deviceId) { 
        bondedDevices.find { it.address == deviceId }?.name ?: "Unknown Device" 
    }

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
                    Text(
                        "Finding: $deviceName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            // Radar View with Sonar Markers
            RadarView(
                peakHeading = peakHeading, 
                peakRssi = peakRssi, 
                currentHeading = heading,
                history = signalHistory
            )

            // Compass Ring
            CompassRing(
                heading = animatedHeading,
                peakHeading = peakHeading,
                peakRssi = peakRssi
            )

            // Pulsing Dot
            PulsingDot(scale = dotScale)

            // Info Column
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val category = getDistanceCategory(rssi)
                
                // Signal Strength Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    SignalStrengthBar(rssi = rssi, modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp))
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.displaySmall,
                        color = when (category) {
                            DistanceCategory.HOT -> Color(0xFFFF4444)
                            DistanceCategory.WARM -> Color(0xFFFFBB33)
                            else -> MaterialTheme.colorScheme.secondary
                        },
                        fontWeight = FontWeight.Black
                    )
                }
                
                // Heading Card
                HeadingCard(
                    heading = heading, 
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Buzz my watch Section
                OutlinedButton(
                    onClick = { /* Coming soon */ },
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SonarCyan.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SonarCyan,
                        disabledContentColor = SonarCyan.copy(alpha = 0.4f)
                    )
                ) {
                    Text("BUZZ MY WATCH", fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Requires Wear OS app (coming soon)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun HeadingCard(heading: Float, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(
                    text = "HEADING",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${heading.roundToInt()}° ${getCardinalDirection(heading)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun SignalStrengthBar(rssi: Int, modifier: Modifier = Modifier) {
    val progress = ((rssi + 100).coerceIn(0, 70) / 70f)
    val color = when {
        rssi > -60 -> SonarGreen
        rssi > -80 -> SonarCyan
        else -> MaterialTheme.colorScheme.error
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "SIGNAL STRENGTH",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Text(
                "${(progress * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun RadarView(
    peakHeading: Float, 
    peakRssi: Int, 
    currentHeading: Float,
    history: List<com.kglabs28.btradiusdetector.ui.SignalPoint> = emptyList()
) {
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepRotation"
    )

    Canvas(modifier = Modifier.size(320.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.width / 2

        // Rings
        for (i in 1..4) {
            drawCircle(
                color = secondaryColor.copy(alpha = 0.2f),
                radius = maxRadius * (i / 4f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // History Sonar Markers
        history.forEachIndexed { index, point ->
            val alpha = (1f - (index / history.size.toFloat())) * 0.4f
            val angleRad = Math.toRadians((point.heading - currentHeading - 90).toDouble())
            val markerRadius = maxRadius * ((point.rssi + 100).coerceIn(0, 70) / 70f)
            val markerX = center.x + markerRadius * kotlin.math.cos(angleRad).toFloat()
            val markerY = center.y + markerRadius * kotlin.math.sin(angleRad).toFloat()

            drawCircle(
                color = secondaryColor.copy(alpha = alpha),
                radius = 4.dp.toPx(),
                center = Offset(markerX, markerY),
                style = Fill
            )
        }

        // Sonar Marker (Peak)
        if (peakRssi > -100) {
            val angleRad = Math.toRadians((peakHeading - currentHeading - 90).toDouble())
            val markerRadius = maxRadius * ((peakRssi + 100).coerceIn(0, 70) / 70f)
            val markerX = center.x + markerRadius * kotlin.math.cos(angleRad).toFloat()
            val markerY = center.y + markerRadius * kotlin.math.sin(angleRad).toFloat()
            
            drawCircle(
                color = Color.Red.copy(alpha = 0.6f),
                radius = 8.dp.toPx(),
                center = Offset(markerX, markerY),
                style = Fill
            )
            drawCircle(
                color = Color.Red,
                radius = 4.dp.toPx(),
                center = Offset(markerX, markerY),
                style = Fill
            )
        }

        // Rotate and draw sweep
        rotate(sweepRotation, center) {
            drawArc(
                brush = Brush.sweepGradient(
                    0.75f to Color.Transparent,
                    1.0f to secondaryColor.copy(alpha = 0.5f),
                    center = center
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = true,
                size = size,
                topLeft = Offset.Zero
            )
            
            // Leading line
            drawLine(
                color = secondaryColor,
                start = center,
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

@Composable
fun CompassRing(heading: Float, peakHeading: Float, peakRssi: Int) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.size(360.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.width / 2 - 20.dp.toPx()

            drawCircle(
                color = primaryColor.copy(alpha = 0.3f),
                radius = radius + 10.dp.toPx(),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Compass Markers (N, E, S, W)
        val markers = listOf("N", "E", "S", "W")
        markers.forEachIndexed { index, label ->
            val angle = (index * 90f) - heading
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(angle),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = label,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Peak Signal Marker
        if (peakRssi > -100) {
            val peakAngle = peakHeading - heading
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(peakAngle),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Best signal\n(${peakHeading.roundToInt()}°)",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PulsingDot(scale: Float) {
    val secondaryColor = MaterialTheme.colorScheme.secondary
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
        // Outer glow
        Canvas(modifier = Modifier.size(60.dp)) {
            drawCircle(
                color = secondaryColor.copy(alpha = 0.2f),
                radius = (size.width / 2) * scale * glowScale
            )
        }
        // Inner dot
        Canvas(modifier = Modifier.size(30.dp)) {
            drawCircle(
                color = secondaryColor,
                radius = (size.width / 2) * scale
            )
            // Center highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = (size.width / 4) * scale
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun TrackingScreenPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Finding: My Device",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                RadarView(peakHeading = 120f, peakRssi = -60, currentHeading = 45f)
                CompassRing(heading = 45f, peakHeading = 120f, peakRssi = -60)
                PulsingDot(scale = 0.8f)

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Signal Strength Section
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        SignalStrengthBar(rssi = -65, modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp))
                        Text(
                            text = "STRONG / HOT",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color(0xFFFF4444),
                            fontWeight = FontWeight.Black
                        )
                    }
                    
                    // Heading Card
                    HeadingCard(
                        heading = 45f, 
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Buzz my watch Section
                    OutlinedButton(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, SonarCyan.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SonarCyan,
                            disabledContentColor = SonarCyan.copy(alpha = 0.4f)
                        )
                    ) {
                        Text("BUZZ MY WATCH", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Requires Wear OS app (coming soon)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
