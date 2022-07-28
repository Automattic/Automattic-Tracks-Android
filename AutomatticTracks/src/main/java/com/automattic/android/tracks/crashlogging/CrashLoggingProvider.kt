package com.automattic.android.tracks.crashlogging

import android.app.Application
import com.automattic.android.tracks.crashlogging.internal.SentryCrashLogging
import com.automattic.android.tracks.crashlogging.internal.SentryErrorTrackerWrapper
import kotlinx.coroutines.CoroutineScope

object CrashLoggingProvider {
    fun createInstance(
        context: Application,
        dataProvider: CrashLoggingDataProvider,
        appScope: CoroutineScope
    ): CrashLogging = SentryCrashLogging(context, dataProvider, SentryErrorTrackerWrapper(), appScope)
}
