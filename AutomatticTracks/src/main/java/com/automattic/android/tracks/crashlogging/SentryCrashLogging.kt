package com.automattic.android.tracks.crashlogging

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.automattic.android.tracks.crashlogging.internal.SentryErrorTrackerWrapper
import com.automattic.android.tracks.crashlogging.internal.toSentryUser
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.protocol.Message

class SentryCrashLogging @VisibleForTesting internal constructor(
    context: Context,
    private val dataProvider: CrashLoggingDataProvider,
    private val sentryWrapper: SentryErrorTrackerWrapper,
) : CrashLogging {

    constructor(
        context: Context,
        dataProvider: CrashLoggingDataProvider,
    ) : this(context, dataProvider, SentryErrorTrackerWrapper())

    init {
        sentryWrapper.initialize(context) { options ->
            options.apply {
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
            if (dataProvider.shouldDropWrappingException(lastException.module, lastException.type, lastException.value)) {
                event.exceptions.remove(lastException)
            }
        }
    }

    override fun appendApplicationContext(newApplicationContext: Map<String, String>) {
        newApplicationContext.forEach { entry ->
            sentryWrapper.applyExtra(entry.key, entry.value)
        }
    }

    override fun log(throwable: Throwable) {
        sentryWrapper.captureException(throwable)
    }

    override fun log(throwable: Throwable, data: Map<String, String?>) {
        val event = SentryEvent().apply {
            message = Message().apply {
                message = throwable.message
            }
            level = SentryLevel.ERROR
            setExtras(data.toMutableMap() as Map<String, String?>)
        }
        sentryWrapper.captureEvent(event)
    }

    override fun log(message: String) {
        sentryWrapper.captureMessage(message)
    }
}
