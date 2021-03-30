package com.automattic.android.tracks.crashlogging.internal

import android.content.Context
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.User

class SentryErrorTrackerWrapper {

    fun initialize(context: Context, configure: (SentryOptions) -> Unit) {
        SentryAndroid.init(context) { options ->
            configure(options)
        }
    }

    fun clearBreadcrumbs() {
        Sentry.clearBreadcrumbs()
    }

    fun setUser(user: User?) {
        Sentry.setUser(user)
    }

    fun captureException(exception: Throwable) {
        Sentry.captureException(exception)
    }

    fun captureEvent(event: SentryEvent) {
        Sentry.captureEvent(event)
    }

    fun captureMessage(message: String) {
        Sentry.captureMessage(message)
    }

    fun applyExtra(key: String, value: String) {
        Sentry.setExtra(key, value)
    }
}
