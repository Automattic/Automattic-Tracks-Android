package com.automattic.android.tracks.crashlogging

import android.content.Context
import com.automattic.android.tracks.crashlogging.internal.SentryCrashLogging
import com.automattic.android.tracks.crashlogging.internal.SentryErrorTrackerWrapper

object CrashLoggingProvider {
    fun createInstance(
        context: Context,
        dataProvider: CrashLoggingDataProvider
    ): CrashLogging = SentryCrashLogging(context, dataProvider, SentryErrorTrackerWrapper())
}
