package com.kglabs28.btradiusdetector.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.kglabs28.btradiusdetector.ui.components.DeviceRow
import com.kglabs28.btradiusdetector.ui.components.EmptyState
import com.kglabs28.btradiusdetector.ui.components.MonitorRangeAlertsCard
import com.kglabs28.btradiusdetector.ui.components.PermissionRationaleDialog
import com.kglabs28.btradiusdetector.ui.components.PermissionRequestContent
import com.kglabs28.btradiusdetector.ui.components.SectionHeader
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onDeviceSelected: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(context))

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

    val bondedDevices by viewModel.bondedDevices.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.homeTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = Dimens.alphaBackgroundTint), Color.Transparent),
                        radius = 1000f
                    )
                )
            )

            if (showRationaleDialog) {
                PermissionRationaleDialog(
                    onConfirm = { showRationaleDialog = false; permissionState.launchMultiplePermissionRequest() },
                    onDismiss = { showRationaleDialog = false }
                )
            }

            if (!permissionState.allPermissionsGranted) {
                PermissionRequestContent(
                    onRequestPermissions = {
                        if (permissionState.shouldShowRationale) showRationaleDialog = true
                        else permissionState.launchMultiplePermissionRequest()
                    }
                )
            } else if (bondedDevices.isEmpty()) {
                EmptyState(message = Strings.noBondedDevices)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Dimens.spacingMd.scaled()),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Dimens.spacingMd.scaled())
                ) {
                    item { SectionHeader(Strings.pairedDevicesHeader) }
                    items(bondedDevices) { device ->
                        DeviceRow(device = device, onClick = { if (device.isConnected) onDeviceSelected(device.address) })
                    }
                    item {
                        Spacer(modifier = Modifier.height(Dimens.spacingXs.scaled()))
                        MonitorRangeAlertsCard(onClick = onSettingsClick)
                    }
                }
            }
        }
    }
}