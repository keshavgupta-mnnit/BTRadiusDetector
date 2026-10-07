package com.kglabs28.btradiusdetector.ui.screens.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kglabs28.btradiusdetector.BTRadiusDetectorApp
import com.kglabs28.btradiusdetector.domain.model.NearbyDevice
import com.kglabs28.btradiusdetector.utils.BluetoothUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Everything the nearby screen renders, observed as one flow. */
data class NearbyUiState(
    val devices: List<NearbyDevice> = emptyList(),
    val bondedAddresses: Set<String> = emptySet()
)

/**
 * Thin wiring only: the unfiltered nearby-radio stream as [uiState].
 * Reads go straight to [BluetoothUtils].
 */
class NearbyViewModel : ViewModel() {

    private val appContext get() = BTRadiusDetectorApp.appContext

    val uiState: StateFlow<NearbyUiState> = combine(
        BluetoothUtils.getNearbyBleFlow(appContext),
        BluetoothUtils.getBondedDevicesFlow(appContext)
    ) { nearby, bonded ->
        NearbyUiState(
            devices = nearby,
            bondedAddresses = bonded.map { it.address }.toSet()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NearbyUiState())

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NearbyViewModel() as T
            }
        }
    }
}
