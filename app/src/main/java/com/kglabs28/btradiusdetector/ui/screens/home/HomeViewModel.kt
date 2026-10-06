package com.kglabs28.btradiusdetector.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kglabs28.btradiusdetector.data.BleRssiRepository
import com.kglabs28.btradiusdetector.domain.model.BluetoothDeviceModel
import com.kglabs28.btradiusdetector.domain.usecase.ObserveBondedDevicesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Everything the home screen renders, observed as one flow. */
data class HomeUiState(
    val devices: List<BluetoothDeviceModel> = emptyList()
)

/**
 * Thin wiring only: exposes the bonded-device stream as [uiState].
 * Refresh and repository access live in [ObserveBondedDevicesUseCase].
 */
class HomeViewModel(
    private val observeBondedDevices: ObserveBondedDevicesUseCase
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = observeBondedDevices()
        .map { devices -> HomeUiState(devices) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun refresh() {
        observeBondedDevices.refresh()
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                return HomeViewModel(
                    ObserveBondedDevicesUseCase(BleRssiRepository(appContext))
                ) as T
            }
        }
    }
}
