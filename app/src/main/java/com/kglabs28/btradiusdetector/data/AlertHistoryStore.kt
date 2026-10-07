package com.kglabs28.btradiusdetector.data

import android.content.Context
import com.kglabs28.btradiusdetector.domain.model.AlertActivity
import com.kglabs28.btradiusdetector.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent notification history (last 50 posted alerts). Backed by
 * preferences so swiped-away or missed alerts survive process death —
 * deliberately not a second Room table: this is an append-only capped log,
 * not relational data. Encoded with framework org.json (no extra dependency).
 */
class AlertHistoryStore private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _history = MutableStateFlow(load())
    val history: StateFlow<List<AlertActivity>> = _history.asStateFlow()

    @Synchronized
    fun record(entry: AlertActivity) {
        // Newest first, capped per device so one chatty bud can't evict
        // everyone else, plus a global safety cap.
        val perDevice = (listOf(entry) + _history.value)
            .groupBy { it.address }
            .flatMap { (_, entries) -> entries.take(Constants.HISTORY_PER_DEVICE) }
        val updated = perDevice.sortedByDescending { it.atMillis }.take(Constants.HISTORY_MAX_TOTAL)
        _history.value = updated
        runCatching {
            val array = JSONArray()
            updated.forEach { array.put(encode(it)) }
            prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
        }
    }

    private fun load(): List<AlertActivity> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i -> decode(array.getJSONObject(i)) }
        }.getOrDefault(emptyList()).take(Constants.HISTORY_MAX_TOTAL)
    }

    private fun encode(entry: AlertActivity): JSONObject = JSONObject()
        .put("address", entry.address)
        .put("deviceName", entry.deviceName)
        .put("event", entry.event)
        .put("posted", entry.posted)
        .put("reason", entry.reason)
        .put("majorClass", entry.majorClass)
        .put("minorClass", entry.minorClass)
        .put("atMillis", entry.atMillis)

    private fun decode(json: JSONObject): AlertActivity = AlertActivity(
        address = json.optString("address"),
        deviceName = json.optString("deviceName"),
        event = json.optString("event"),
        posted = json.optBoolean("posted"),
        reason = json.optString("reason"),
        majorClass = json.optInt("majorClass"),
        minorClass = json.optInt("minorClass"),
        atMillis = json.optLong("atMillis")
    )

    companion object {
        private const val PREFS_NAME = "alert_history"
        private const val KEY_HISTORY = "history_v1"

        @Volatile
        private var instance: AlertHistoryStore? = null

        fun getInstance(context: Context): AlertHistoryStore =
            instance ?: synchronized(this) {
                instance ?: AlertHistoryStore(context).also { instance = it }
            }
    }
}
