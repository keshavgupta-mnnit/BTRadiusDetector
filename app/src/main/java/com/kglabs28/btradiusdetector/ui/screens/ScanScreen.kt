package com.kglabs28.btradiusdetector.ui.screens

import android.Manifest
import android.bluetooth.BluetoothClass
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.kglabs28.btradiusdetector.domain.model.BluetoothDeviceModel
import com.kglabs28.btradiusdetector.ui.MainViewModel
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: MainViewModel, onDeviceSelected: (String) -> Unit, onSettingsClick: () -> Unit
) {
    val permissionsToRequest = remember {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN)
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            list.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list
    }

    val permissionState = rememberMultiplePermissionsState(permissionsToRequest)
    var showRationaleDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            viewModel.refreshBondedDevices()

            // Start the monitoring service if permissions are granted
//            val intent = Intent(context, MonitoringService::class.java)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                context.startForegroundService(intent)
//            } else {
//                context.startService(intent)
//            }
        }
    }

    val bondedDevices by viewModel.bondedDevices.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                Text(
                    "SELECT DEVICE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }, actions = {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                }
            }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
            )
        }, containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Background Sonar Glow Effect (simplified)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                Color.Transparent
                            ), radius = 1000f
                        )
                    )
            )

            if (showRationaleDialog) {
                PermissionRationaleDialog(onConfirm = {
                    showRationaleDialog = false
                    permissionState.launchMultiplePermissionRequest()
                }, onDismiss = {
                    showRationaleDialog = false
                })
            }

            if (!permissionState.allPermissionsGranted) {
                PermissionRequestContent(
                    onRequestPermissions = {
                        if (permissionState.shouldShowRationale) {
                            showRationaleDialog = true
                        } else {
                            permissionState.launchMultiplePermissionRequest()
                        }
                    })
            } else {
//                val preferences by viewModel.userPreferences.collectAsState()
//                val monitoredDevices = preferences?.monitoredDevices ?: emptySet()
//
//                if (bondedDevices.isEmpty()) {
//                    EmptyDevicesContent(onRefresh = { viewModel.refreshBondedDevices() })
//                } else {
//                    DeviceList(
//                        devices = bondedDevices,
//                        monitoredDevices = monitoredDevices,
//                        onDeviceClick = onDeviceSelected,
//                        onToggleMonitoring = { address, enabled ->
//                            viewModel.toggleMonitoring(address, enabled)
//                        }
//                    )
//                }
            }
        }
    }
}

@Composable
fun DeviceList(
    devices: List<BluetoothDeviceModel>,
    monitoredDevices: Set<String>,
    onDeviceClick: (String) -> Unit,
    onToggleMonitoring: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "BONDED DEVICES",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(devices) { device ->
            DeviceRow(
                device = device,
                isMonitored = monitoredDevices.contains(device.address),
                onClick = { onDeviceClick(device.address) },
                onToggleMonitoring = { enabled -> onToggleMonitoring(device.address, enabled) })
        }
    }
}

@Composable
fun DeviceRow(
    device: BluetoothDeviceModel,
    isMonitored: Boolean,
    onClick: () -> Unit,
    onToggleMonitoring: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getDeviceIcon(device.deviceClass),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "Unknown Device",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (device.isConnected) "Connected" else "Paired",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (device.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = 0.7f
                    )
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

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PermissionRationaleDialog(
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Permissions Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "Bluetooth permissions are needed to find and connect to your devices. " + "Location is required for scanning on older Android versions. " + "Notifications are used for range alerts.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.primary,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun PermissionRequestContent(onRequestPermissions: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.Bluetooth,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Permissions Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Bluetooth and Location permissions are needed to find and track your devices. Notifications are used for range alerts.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRequestPermissions,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("GRANT PERMISSIONS")
        }
    }
}

@Composable
fun EmptyDevicesContent(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "No Bonded Devices Found",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onRefresh) {
            Text("REFRESH")
        }
    }
}

fun getDeviceIcon(majorClass: Int): ImageVector {
    return when (majorClass) {
        BluetoothClass.Device.Major.AUDIO_VIDEO -> Icons.Rounded.Headset
        BluetoothClass.Device.Major.WEARABLE -> Icons.Rounded.Watch
        BluetoothClass.Device.Major.COMPUTER -> Icons.Rounded.Computer
        BluetoothClass.Device.Major.PHONE -> Icons.Rounded.PhoneAndroid
        else -> Icons.Rounded.Bluetooth
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun ScanScreenPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        // Mocking content for preview
        val mockDevices = listOf(
            BluetoothDeviceModel(
                "00:11:22:33:44:55",
                "Sony WH-1000XM4",
                BluetoothClass.Device.Major.AUDIO_VIDEO,
                0,
                true
            ), BluetoothDeviceModel(
                "AA:BB:CC:DD:EE:FF", "Pixel Watch 2", BluetoothClass.Device.Major.WEARABLE, 0, false
            ), BluetoothDeviceModel(
                "12:34:56:78:90:AB", "MacBook Pro", BluetoothClass.Device.Major.COMPUTER, 0, false
            ), BluetoothDeviceModel(
                "55:44:33:22:11:00", "Pixel 8 Pro", BluetoothClass.Device.Major.PHONE, 0, true
            )
        )

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                    Text(
                        "SELECT DEVICE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }, actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
                )
            }, containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    Color.Transparent
                                ), radius = 1000f
                            )
                        )
                )
                DeviceList(
                    devices = mockDevices,
                    monitoredDevices = setOf(mockDevices[0].address),
                    onDeviceClick = {},
                    onToggleMonitoring = { _, _ -> })
            }
        }
    }
}
