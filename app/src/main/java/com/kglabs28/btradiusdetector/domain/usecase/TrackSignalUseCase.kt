package com.kglabs28.btradiusdetector.domain.usecase

import com.kglabs28.btradiusdetector.data.AlertSettingsRepository
import com.kglabs28.btradiusdetector.domain.model.SignalPoint
import com.kglabs28.btradiusdetector.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * The peak-tracking engine: eats (rssi, heading) pairs, keeps the strongest
 * bearing, a bounded history, and persists the best direction per device.
 * This is the complex part of tracking — it lives here, not in a ViewModel.
 */
class TrackSignalUseCase(private val settingsRepository: AlertSettingsRepository) {

    data class Snapshot(
        val peakRssi: Int = Constants.RSSI_FLOOR,
        val peakHeading: Float = 0f,
        val history: List<SignalPoint> = emptyList()
    )

    private val peakRssi = MutableStateFlow(Constants.RSSI_FLOOR)
    private val peakHeading = MutableStateFlow(0f)
    private val history = MutableStateFlow<List<SignalPoint>>(emptyList())

    val snapshot: Flow<Snapshot> = combine(peakRssi, peakHeading, history) { rssi, heading, history ->
        Snapshot(rssi, heading, history)
    }

    fun observeSnapshot(): Flow<Snapshot> = snapshot

    /** Restores the persisted best direction so the radar never opens blank. */
    suspend fun restore(address: String) {
        val saved = settingsRepository.getByAddress(address)
        if (saved != null && saved.lastBestRssi > Constants.RSSI_FLOOR) {
            peakRssi.value = saved.lastBestRssi
            peakHeading.value = saved.lastBestHeading
        }
        history.value = emptyList()
    }

    /** Feeds one reading. New peaks persist; anything at/below floor is noise. */
    suspend fun ingest(address: String, rssi: Int, heading: Float) {
        if (rssi <= Constants.RSSI_FLOOR) return
        if (rssi > peakRssi.value) {
            peakRssi.value = rssi
            peakHeading.value = heading
            settingsRepository.saveBestDirection(address, rssi, heading)
        }
        val updated = history.value.toMutableList()
        updated.add(0, SignalPoint(rssi, heading, System.currentTimeMillis()))
        if (updated.size > Constants.SIGNAL_HISTORY_MAX) updated.removeAt(updated.size - 1)
        history.value = updated
    }

    fun reset() {
        peakRssi.value = Constants.RSSI_FLOOR
        peakHeading.value = 0f
        history.value = emptyList()
    }
}
