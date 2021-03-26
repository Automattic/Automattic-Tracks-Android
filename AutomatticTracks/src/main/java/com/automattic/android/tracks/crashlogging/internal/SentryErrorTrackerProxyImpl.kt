package com.automattic.android.tracks.crashlogging.internal

import android.content.Context
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.User

class SentryErrorTrackerProxyImpl : SentryErrorTrackerProxy {

    override fun initialize(context: Context, configure: (SentryOptions) -> Unit) {
        SentryAndroid.init(context) { options ->
            configure(options)
        }
    }

    override fun clearBreadcrumbs() {
        Sentry.clearBreadcrumbs()
    }

    override fun setUser(user: User?) {
        Sentry.setUser(user)
    }

    override fun captureException(exception: Throwable) {
        Sentry.captureException(exception)
    }

    override fun captureEvent(event: SentryEvent) {
        Sentry.captureEvent(event)
    }

    override fun captureMessage(message: String) {
        Sentry.captureMessage(message)
    }

    override fun applyExtra(key: String, value: String) {
        Sentry.setExtra(key, value)
    }
}
