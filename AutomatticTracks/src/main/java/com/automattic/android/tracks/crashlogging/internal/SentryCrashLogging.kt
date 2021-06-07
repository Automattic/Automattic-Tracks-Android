package com.automattic.android.tracks.crashlogging.internal

import android.content.Context
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.ExtraKnownKey
import com.automattic.android.tracks.crashlogging.eventLevel
import io.sentry.Breadcrumb
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

                    if (!dataProvider.crashLoggingEnabled()) return@BeforeSendCallback null

                    dropExceptionIfRequired(event)
                    appendExtra(event)
                    appendApplicationContext(event)
                    appendUser(event)
                    event
                }
            }
        }
    }

    private fun appendUser(event: SentryEvent) {
        event.user = dataProvider.userProvider()?.toSentryUser()
    }

    private fun appendApplicationContext(event: SentryEvent) {
        event.appendTags(dataProvider.applicationContextProvider())
    }

    private fun appendExtra(event: SentryEvent) {
        event.setExtras(
            dataProvider.provideExtrasForEvent(
                currentExtras = mergeKnownKeysWithValues(event),
                eventLevel = event.eventLevel
            )
        )
    }

    private fun mergeKnownKeysWithValues(event: SentryEvent): Map<ExtraKnownKey, String> =
        dataProvider.extraKnownKeys()
            .associateWith { knownKey -> event.getExtra(knownKey) }
            .filterValues { it != null }
            .mapValues(Any::toString)

    private fun dropExceptionIfRequired(event: SentryEvent) {
        event.exceptions?.lastOrNull()?.let { lastException ->
            if (dataProvider.shouldDropWrappingException(
                    lastException.module.orEmpty(),
                    lastException.type.orEmpty(),
                    lastException.value.orEmpty(),
                )
            ) {
                event.exceptions?.remove(lastException)
            }
        }
    }

    override fun recordEvent(message: String, category: String?) {
        val breadcrumb = Breadcrumb().apply {
            this.category = category
            this.type = "default"
            this.message = message
            this.level = SentryLevel.INFO
        }
        sentryWrapper.addBreadcrumb(breadcrumb)
    }

    override fun recordException(exception: Throwable, category: String?) {
        val breadcrumb = Breadcrumb().apply {
            this.category = category
            this.type = "error"
            this.message = exception.toString()
            this.level = SentryLevel.ERROR
        }
        sentryWrapper.addBreadcrumb(breadcrumb)
    }

    override fun sendReport(exception: Throwable?, tags: Map<String, String>, message: String?) {
        val event = SentryEvent(exception).apply {
            this.message = Message().apply { this.message = message }
            this.level = if (exception != null) SentryLevel.ERROR else SentryLevel.INFO
            this.appendTags(tags)
        }
        sentryWrapper.captureEvent(event)
    }
}
