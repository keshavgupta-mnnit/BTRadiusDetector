package com.kglabs28.btradiusdetector.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kglabs28.btradiusdetector.BTRadiusDetectorApp
import com.kglabs28.btradiusdetector.domain.model.BluetoothDeviceModel
import com.kglabs28.btradiusdetector.utils.BluetoothUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Everything the home screen renders, observed as one flow. */
data class HomeUiState(
    val devices: List<BluetoothDeviceModel> = emptyList()
)

/**
 * Thin wiring only: bonded-device stream as [uiState].
 * Bluetooth reads go straight to [BluetoothUtils].
 */
class HomeViewModel : ViewModel() {

    private val appContext get() = BTRadiusDetectorApp.appContext

    private val refreshTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = refreshTrigger
        .flatMapLatest { BluetoothUtils.getBondedDevicesFlow(appContext) }
        .map { devices -> HomeUiState(devices) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun refresh() {
        refreshTrigger.value += 1
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel() as T
            }
        }
    }
}
