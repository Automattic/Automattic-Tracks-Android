package com.automattic.android.tracks.crashlogging

import android.app.Application
import com.automattic.android.tracks.crashlogging.internal.SentryCrashLogging
import com.automattic.android.tracks.crashlogging.internal.SentryErrorTrackerWrapper
import kotlinx.coroutines.CoroutineScope

object CrashLoggingProvider {
    fun createInstance(
        application: Application,
        dataProvider: CrashLoggingDataProvider,
        applicationScope: CoroutineScope
    ): CrashLogging = SentryCrashLogging(application, dataProvider, SentryErrorTrackerWrapper(), applicationScope)
}
