package com.automattic.android.tracks.crashlogging

import android.app.Application
import com.automattic.android.tracks.crashlogging.internal.SentryCrashLogging
import com.automattic.android.tracks.crashlogging.internal.SentryErrorTrackerWrapper

object CrashLoggingProvider {
    fun createInstance(
        application: Application,
        dataProvider: CrashLoggingDataProvider
    ): CrashLogging = SentryCrashLogging(application, dataProvider, SentryErrorTrackerWrapper())
}
