package com.kglabs28.btradiusdetector.utils

import android.util.Log
import com.kglabs28.btradiusdetector.BuildConfig

/**
 * Debug-only logging. Every call compiles into the release APK as a no-op
 * branch on a constant `false`, so R8 strips both the check and the message
 * — zero log leakage and zero cost in release builds.
 *
 * Filter logcat with: adb logcat -s BTRadius:V
 * (all tags below are prefixed; grep "BTRadius" to follow one repro).
 */
object LogUtils {

    /** Tag prefix shared by the whole alert pipeline. */
    const val PREFIX = "BTRadius"

    fun tag(name: String): String = "$PREFIX:$name"

    @JvmStatic
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }

    @JvmStatic
    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.i(tag, message)
    }

    @JvmStatic
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.w(tag, message, throwable)
    }

    @JvmStatic
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.e(tag, message, throwable)
    }
}
