package com.kglabs28.btradiusdetector.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kglabs28.btradiusdetector.BTRadiusDetectorApp
import com.kglabs28.btradiusdetector.data.AlertHistoryStore
import com.kglabs28.btradiusdetector.domain.model.AlertActivity
import com.kglabs28.btradiusdetector.utils.Strings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Everything the history screen renders, observed as one flow. */
data class HistoryUiState(
    val entries: List<AlertActivity> = emptyList(),
    val devices: List<DeviceFilter> = emptyList(),
    val selectedAddress: String? = null
)

/** One filter chip: null address means "All". */
data class DeviceFilter(
    val address: String?,
    val name: String
)

/**
 * Thin wiring only: history stream plus the selected device filter.
 * Reads go straight to [AlertHistoryStore].
 */
class HistoryViewModel : ViewModel() {

    private val store = AlertHistoryStore.getInstance(BTRadiusDetectorApp.appContext)

    private val _selectedAddress = MutableStateFlow<String?>(null)
    val selectedAddress = _selectedAddress.asStateFlow()

    val uiState: StateFlow<HistoryUiState> = combine(
        store.history, selectedAddress
    ) { history, selected ->
        HistoryUiState(
            entries = if (selected == null) history else history.filter { it.address == selected },
            devices = listOf(DeviceFilter(null, Strings.historyFilterAll)) +
                history.map { it.address }.distinct().map { address ->
                    DeviceFilter(address, history.first { it.address == address }.deviceName)
                },
            selectedAddress = selected
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    fun selectDevice(address: String?) {
        _selectedAddress.value = address
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel() as T
            }
        }
    }
}
