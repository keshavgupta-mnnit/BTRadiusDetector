package com.kglabs28.btradiusdetector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.CompassCalibration
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kglabs28.btradiusdetector.ui.MainViewModel
import com.kglabs28.btradiusdetector.ui.theme.SonarCyan
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(viewModel: MainViewModel) {
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome to BT Radius",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = SonarCyan,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                OnboardingItem(
                    icon = Icons.AutoMirrored.Rounded.BluetoothSearching,
                    title = "Select Device",
                    description = "Choose a paired Bluetooth device to track its signal strength."
                )

                Spacer(modifier = Modifier.height(16.dp))

                OnboardingItem(
                    icon = Icons.Rounded.CompassCalibration,
                    title = "Rotate to Find",
                    description = "Slowly rotate your phone. The compass will track the best signal direction."
                )

                Spacer(modifier = Modifier.height(16.dp))

                OnboardingItem(
                    icon = Icons.Rounded.SignalCellularAlt,
                    title = "Proximity Meter",
                    description = "Watch the RSSI values. Hot means you're very close!"
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        scope.launch {
                            viewModel.updateOnboardingCompleted()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SonarCyan)
                ) {
                    Text(
                        "GOT IT",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingItem(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SonarCyan,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}
