package com.kglabs28.btradiusdetector.ui.screens.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kglabs28.btradiusdetector.ui.components.HistoryFilterChips
import com.kglabs28.btradiusdetector.ui.components.HistoryList
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.utils.Dimens
import com.kglabs28.btradiusdetector.utils.Strings
import com.kglabs28.btradiusdetector.utils.scaled

/**
 * Notification history: last 50 posted alerts, newest first, filterable by
 * device. The safety net for swiped-away or missed notifications.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit
) {
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.factory())
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(Strings.historyTitle, fontWeight = FontWeight.Bold, fontSize = Dimens.textTitleSmall)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = Strings.backDesc)
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
            modifier = Modifier.fillMaxSize().padding(padding)
                .padding(horizontal = Dimens.screenPaddingH.scaled())
                .verticalScroll(rememberScrollState())
        ) {
            HistoryFilterChips(
                devices = state.devices,
                selectedAddress = state.selectedAddress,
                onSelect = viewModel::selectDevice
            )
            Spacer(modifier = Modifier.height(Dimens.spacingMd.scaled()))
            HistoryList(entries = state.entries)
            Spacer(modifier = Modifier.height(Dimens.spacingLg.scaled()))
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun HistoryScreenPreview() {
    BTRadiusDetectorTheme(darkTheme = true) {
        HistoryScreen(onBack = {})
    }
}
