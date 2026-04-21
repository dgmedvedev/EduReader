package com.example.edureader.core.monitoring

import android.app.Application
import com.example.edureader.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class MonitoringInitializer @Inject constructor(
    private val appLogger: AppLogger
) {
    fun init() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        runCatching {
            FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
        }.onFailure {
            Timber.tag(TAG).w(it, "Crashlytics is unavailable. Continuing with Timber only.")
        }
        appLogger.i(
            TAG,
            "Monitoring initialized for ${if (BuildConfig.DEBUG) "debug" else "release"} build"
        )
    }

    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority < android.util.Log.INFO) return
            runCatching {
                val crashlytics = FirebaseCrashlytics.getInstance()
                crashlytics.log("${tag ?: "App"}: $message")
                if (t != null && priority >= android.util.Log.WARN) {
                    crashlytics.recordException(t)
                }
            }.onFailure {
                android.util.Log.println(priority, tag ?: "App", message)
            }
        }
    }

    private companion object {
        private const val TAG = "MonitoringInitializer"
    }
}
