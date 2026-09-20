package com.kglabs28.btradiusdetector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kglabs28.btradiusdetector.domain.model.BluetoothDeviceModel
import com.kglabs28.btradiusdetector.ui.MainViewModel
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val bondedDevices by viewModel.bondedDevices.collectAsState()
    val preferences by viewModel.userPreferences.collectAsState()
    val monitoredDevices = preferences?.monitoredDevices ?: emptySet()

    LaunchedEffect(Unit) {
        viewModel.refreshBondedDevices()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "SETTINGS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
                .padding(padding)
        ) {
            // Background Sonar Glow Effect
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            radius = 1000f
                        )
                    )
            )

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "RANGE ALERTS",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Get notified when these devices connect or disconnect.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    if (bondedDevices.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No bonded devices found",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(bondedDevices) { device ->
                            SettingsDeviceRow(
                                device = device,
                                isMonitored = monitoredDevices.contains(device.address),
                                onToggleMonitoring = { enabled ->
                                    viewModel.toggleMonitoring(device.address, enabled)
                                }
                            )
                        }
                    }
                }

                // Watch Section
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, 
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Rounded.Watch,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Watch Companion",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Requires Wear OS app (coming soon)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { /* Disabled */ },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        ) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("BUZZ MY WATCH")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDeviceRow(
    device: BluetoothDeviceModel,
    isMonitored: Boolean,
    onToggleMonitoring: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getDeviceIcon(device.deviceClass),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "Unknown Device",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Switch(
                checked = isMonitored,
                onCheckedChange = onToggleMonitoring,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun SettingsScreenPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        // We can't easily mock MainViewModel here without an interface or similar,
        // but for preview purposes we can use a dummy content or just see how it looks.
        // For a better preview, we should've extracted the content to a stateless composable.
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
             Text("Settings Preview - requires mock ViewModel", modifier = Modifier.align(Alignment.Center))
        }
    }
}
