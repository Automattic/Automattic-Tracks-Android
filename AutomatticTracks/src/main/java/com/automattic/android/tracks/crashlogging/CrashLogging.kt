package com.automattic.android.tracks.crashlogging

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.automattic.android.tracks.crashlogging.internal.SentryErrorTrackerWrapper
import com.automattic.android.tracks.crashlogging.internal.toSentryUser
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.protocol.Message

object CrashLogging {

    private lateinit var dataProvider: CrashLoggingDataProvider
    private lateinit var sentryWrapper: SentryErrorTrackerWrapper
    private var userOptOut: Boolean = true

    @JvmStatic
    fun start(
        context: Context,
        dataProvider: CrashLoggingDataProvider,
        userHasOptOut: Boolean,
    ) {
        start(context, dataProvider, SentryErrorTrackerWrapper(), userHasOptOut)
    }

    @VisibleForTesting
    internal fun start(
        context: Context,
        dataProvider: CrashLoggingDataProvider,
        sentryWrapper: SentryErrorTrackerWrapper,
        userHasOptOut: Boolean,
    ) {
        this.sentryWrapper = sentryWrapper
        this.dataProvider = dataProvider
        this.userOptOut = userHasOptOut

        initialize(context)
    }

    private fun initialize(context: Context) {
        sentryWrapper.initialize(context) { options ->
            options.apply {
                dsn = dataProvider.sentryDSN
                environment = dataProvider.buildType
                release = dataProvider.releaseName
                setDebug(dataProvider.enableCrashLoggingLogs)
                setTag("locale", dataProvider.locale?.language ?: "unknown")
                beforeSend = SentryOptions.BeforeSendCallback { event, _ ->
                    return@BeforeSendCallback if (userOptOut) {
                        null
                    } else {
                        event.apply {
                            user = dataProvider.userProvider()?.toSentryUser()
                        }
                    }
                }
            }
        }
    }

    fun updateUserOptOutPreference(userHasOptOut: Boolean) {
        this.userOptOut = userHasOptOut
    }

    fun appendApplicationContext(newApplicationContext: Map<String, String>) {
        newApplicationContext.forEach { entry ->
            sentryWrapper.applyExtra(entry.key, entry.value)
        }
    }

    @JvmStatic
    fun log(throwable: Throwable) {
        sentryWrapper.captureException(throwable)
    }

    @JvmStatic
    fun log(throwable: Throwable, data: Map<String, String?>) {
        val event = SentryEvent().apply {
            message = Message().apply {
                message = throwable.message
            }
            level = SentryLevel.ERROR
            setExtras(data.toMutableMap() as Map<String, String?>)
        }
        sentryWrapper.captureEvent(event)
    }

    @JvmStatic
    fun log(message: String) {
        sentryWrapper.captureMessage(message)
    }
}
