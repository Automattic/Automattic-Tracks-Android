package com.automattic.android.tracks.crashlogging

import io.sentry.SentryEvent
import io.sentry.SentryLevel.DEBUG
import io.sentry.SentryLevel.ERROR
import io.sentry.SentryLevel.FATAL
import io.sentry.SentryLevel.INFO
import io.sentry.SentryLevel.WARNING

/**
 * Maps [io.sentry.SentryLevel]
 */
enum class EventLevel {
    DEBUG, INFO, WARNING, ERROR, FATAL
}

val SentryEvent.eventLevel: EventLevel
    get() {
        return when (level) {
            DEBUG -> EventLevel.DEBUG
            INFO -> EventLevel.INFO
            WARNING -> EventLevel.WARNING
            ERROR -> EventLevel.ERROR
            FATAL -> EventLevel.FATAL
            null -> EventLevel.DEBUG
        }
    }
