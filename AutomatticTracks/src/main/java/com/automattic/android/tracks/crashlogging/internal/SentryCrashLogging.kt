package com.automattic.android.tracks.crashlogging.internal

import android.content.Context
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.eventLevel
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.protocol.Message

internal class SentryCrashLogging constructor(
    context: Context,
    private val dataProvider: CrashLoggingDataProvider,
    private val sentryWrapper: SentryErrorTrackerWrapper,
) : CrashLogging {

    init {
        sentryWrapper.initialize(context) { options ->
            options.apply {
                dsn = dataProvider.sentryDSN
                environment = dataProvider.buildType
                release = dataProvider.releaseName
                setDebug(dataProvider.enableCrashLoggingLogs)
                setTag("locale", dataProvider.locale?.language ?: "unknown")
                beforeSend = SentryOptions.BeforeSendCallback { event, _ ->

                    if (dataProvider.userHasOptOutProvider()) return@BeforeSendCallback null

                    dropExceptionIfRequired(event)
                    appendExtra(event)
                    event.apply {
                        user = dataProvider.userProvider()?.toSentryUser()
                    }
                }
            }
        }
    }

    private fun appendExtra(event: SentryEvent) {
        event.setExtras(
            dataProvider.provideExtrasForEvent(
                currentExtras = mergeKnownKeysWithValues(event),
                eventLevel = event.eventLevel
            )
        )
    }

    private fun mergeKnownKeysWithValues(event: SentryEvent) = dataProvider.extraKnownKeys()
        .associateWith { knownKey -> event.getExtra(knownKey).toString() }

    private fun dropExceptionIfRequired(event: SentryEvent) {
        event.exceptions?.lastOrNull()?.let { lastException ->
            if (dataProvider.shouldDropWrappingException(
                    lastException.module,
                    lastException.type,
                    lastException.value
                )
            ) {
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
