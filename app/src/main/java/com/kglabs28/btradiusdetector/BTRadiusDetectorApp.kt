package com.kglabs28.btradiusdetector

import android.app.Application
import android.content.Context
import com.kglabs28.btradiusdetector.domain.usecase.EnsureAlertUseCase
import com.kglabs28.btradiusdetector.utils.NotificationUtils
import com.kglabs28.btradiusdetector.utils.ScreenScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BTRadiusDetectorApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        ScreenScale.init(this)
        NotificationUtils.createNotificationChannels(this)
        // Reconcile drops recorded while no process survived to post them.
        appScope.launch {
            runCatching { EnsureAlertUseCase.create().reconcilePending() }
        }
    }

    companion object {
        @Volatile
        private var instance: BTRadiusDetectorApp? = null

        /** Application context for utils and ViewModel factories — never an Activity. */
        val appContext: Context
            get() = instance?.applicationContext
                ?: throw IllegalStateException("Application not created yet")
    }
}