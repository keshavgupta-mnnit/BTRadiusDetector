package com.kglabs28.btradiusdetector.data

import android.content.Context

/**
 * Synchronous pending-disconnect flags. Written on the receiver's main thread
 * the moment a drop is seen, reconciled whenever any app process runs
 * (service start, worker pass). Survives process death; the timestamp bounds
 * the window so ancient drops never spam.
 */
class DisconnectIntentStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Must stay main-thread safe: apply() persists asynchronously. */
    fun markDisconnected(address: String) {
        prefs.edit().putLong(keyFor(address), System.currentTimeMillis()).apply()
    }

    fun clear(address: String) {
        prefs.edit().remove(keyFor(address)).apply()
    }

    /** Fresh (recent, unhandled) pending drops. */
    fun freshPending(staleAfterMs: Long): Map<String, Long> {
        return prefs.all.mapNotNull { (key, value) ->
            val address = key.removePrefix(KEY_PREFIX).takeIf { key.startsWith(KEY_PREFIX) } ?: return@mapNotNull null
            val at = (value as? Long) ?: return@mapNotNull null
            if (System.currentTimeMillis() - at <= staleAfterMs) address to at else null
        }.toMap()
    }

    fun dropStale(staleAfterMs: Long) {
        val now = System.currentTimeMillis()
        val edit = prefs.edit()
        var changed = false
        prefs.all.forEach { (key, value) ->
            if (!key.startsWith(KEY_PREFIX)) return@forEach
            val at = (value as? Long) ?: 0L
            if (now - at > staleAfterMs) {
                edit.remove(key)
                changed = true
            }
        }
        if (changed) edit.apply()
    }

    companion object {
        private const val PREFS_NAME = "disconnect_intents"
        private const val KEY_PREFIX = "pending_"
        private fun keyFor(address: String) = KEY_PREFIX + address
    }
}
