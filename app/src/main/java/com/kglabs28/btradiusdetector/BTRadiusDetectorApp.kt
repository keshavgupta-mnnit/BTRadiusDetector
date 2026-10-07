package com.kglabs28.btradiusdetector

import android.app.Application
import com.kglabs28.btradiusdetector.service.MonitoringService
import com.kglabs28.btradiusdetector.utils.NotificationUtils
import com.kglabs28.btradiusdetector.utils.ScreenScale

class BTRadiusDetectorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ScreenScale.init(this)
        NotificationUtils.createNotificationChannels(this)
        // The monitor owns disconnect delivery even when the UI never runs.
        MonitoringService.ensure(this)
    }
}