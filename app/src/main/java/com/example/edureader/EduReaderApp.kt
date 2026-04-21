package com.example.edureader

import android.app.Application
import com.example.edureader.core.monitoring.MonitoringInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class EduReaderApp : Application() {

    @Inject
    lateinit var monitoringInitializer: MonitoringInitializer

    override fun onCreate() {
        super.onCreate()
        monitoringInitializer.init()
    }
}
