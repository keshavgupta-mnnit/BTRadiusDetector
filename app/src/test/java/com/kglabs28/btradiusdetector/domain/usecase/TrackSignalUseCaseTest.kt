package com.kglabs28.btradiusdetector.domain.usecase

import com.kglabs28.btradiusdetector.data.AlertSettingsRepository
import com.kglabs28.btradiusdetector.data.local.AlertSettingsDao
import com.kglabs28.btradiusdetector.data.local.AlertSettingsEntity
import com.kglabs28.btradiusdetector.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeAlertSettingsDao : AlertSettingsDao {
    private val rows = mutableMapOf<String, AlertSettingsEntity>()
    private val stream = MutableStateFlow<List<AlertSettingsEntity>>(emptyList())

    override fun observeAll(): Flow<List<AlertSettingsEntity>> = stream

    override suspend fun getByAddress(address: String): AlertSettingsEntity? = rows[address]

    override suspend fun upsert(entity: AlertSettingsEntity) {
        rows[entity.address] = entity
        stream.value = rows.values.toList()
    }

    override suspend fun delete(entity: AlertSettingsEntity) {
        rows.remove(entity.address)
        stream.value = rows.values.toList()
    }
}

class TrackSignalUseCaseTest {

    private fun useCase(dao: FakeAlertSettingsDao = FakeAlertSettingsDao()) =
        TrackSignalUseCase(AlertSettingsRepository(dao)) to dao

    @Test
    fun `ingest tracks peak and history, ignores floor noise`() = runTest {
        val (tracker, _) = useCase()
        tracker.ingest("AA", Constants.RSSI_FLOOR, 0f)

        val initial = tracker.observeSnapshot().first()
        assertEquals(Constants.RSSI_FLOOR, initial.peakRssi)
        assertTrue(initial.history.isEmpty())

        tracker.ingest("AA", -70, 90f)
        tracker.ingest("AA", -50, 180f)
        tracker.ingest("AA", -80, 270f) // weaker: history only, peak stays

        val snapshot = tracker.observeSnapshot().first()
        assertEquals(-50, snapshot.peakRssi)
        assertEquals(180f, snapshot.peakHeading)
        assertEquals(3, snapshot.history.size)
        assertEquals(-80, snapshot.history.first().rssi) // newest first
    }

    @Test
    fun `new peak persists best direction`() = runTest {
        val (tracker, dao) = useCase()
        tracker.ingest("AA", -55, 42f)

        val saved = dao.getByAddress("AA")
        assertEquals(-55, saved?.lastBestRssi)
        assertEquals(42f, saved?.lastBestHeading)
    }

    @Test
    fun `restore reloads persisted best`() = runTest {
        val dao = FakeAlertSettingsDao()
        val (tracker, _) = useCase(dao)
        dao.upsert(AlertSettingsEntity(address = "AA", lastBestRssi = -52, lastBestHeading = 42f))

        tracker.restore("AA")

        val snapshot = tracker.observeSnapshot().first()
        assertEquals(-52, snapshot.peakRssi)
        assertEquals(42f, snapshot.peakHeading)
    }

    @Test
    fun `history is bounded`() = runTest {
        val (tracker, _) = useCase()
        repeat(Constants.SIGNAL_HISTORY_MAX + 5) { i ->
            tracker.ingest("AA", -60 + (i % 5), i.toFloat())
        }
        assertEquals(Constants.SIGNAL_HISTORY_MAX, tracker.observeSnapshot().first().history.size)
    }

    @Test
    fun `reset clears everything`() = runTest {
        val (tracker, _) = useCase()
        tracker.ingest("AA", -50, 10f)
        tracker.reset()

        val snapshot = tracker.observeSnapshot().first()
        assertEquals(Constants.RSSI_FLOOR, snapshot.peakRssi)
        assertTrue(snapshot.history.isEmpty())
    }
}
