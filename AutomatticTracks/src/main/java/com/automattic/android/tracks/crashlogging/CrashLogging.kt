package com.automattic.android.tracks.crashlogging

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.automattic.android.tracks.crashlogging.internal.SentryErrorTrackerWrapper
import com.automattic.android.tracks.crashlogging.internal.toSentryUser
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.SpanStatus
import io.sentry.protocol.Message


object CrashLogging {

    private lateinit var dataProvider: CrashLoggingDataProvider
    private lateinit var sentryWrapper: SentryErrorTrackerWrapper

    @JvmStatic
    fun start(
        context: Context,
        dataProvider: CrashLoggingDataProvider,
    ) {
        start(context, dataProvider, SentryErrorTrackerWrapper())
    }

    @VisibleForTesting
    internal fun start(
        context: Context,
        dataProvider: CrashLoggingDataProvider,
        sentryWrapper: SentryErrorTrackerWrapper,
    ) {
        this.sentryWrapper = sentryWrapper
        this.dataProvider = dataProvider

        initialize(context)
    }

    private fun initialize(context: Context) {
        sentryWrapper.initialize(context) { options ->
            options.apply {
                tracesSampleRate = 1.0
                dsn = dataProvider.sentryDSN
                environment = dataProvider.buildType
                release = dataProvider.releaseName
                setDebug(dataProvider.enableCrashLoggingLogs)
                setTag("locale", dataProvider.locale?.language ?: "unknown")
                beforeSend = SentryOptions.BeforeSendCallback { event, _ ->
                    dropExceptionIfRequired(event)
                    return@BeforeSendCallback if (dataProvider.userHasOptOutProvider()) {
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

    private fun dropExceptionIfRequired(event: SentryEvent) {
        event.exceptions?.lastOrNull()?.let { lastException ->
            if (dataProvider.toDropIfLastException?.isEqualTo(lastException) == true) {
                event.exceptions.remove(lastException)
            }
        }
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
