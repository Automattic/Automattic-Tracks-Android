package com.automattic.android.tracks.crashlogging.internal

import android.app.Application
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.User

internal class SentryErrorTrackerWrapper {

    fun initialize(context: Application, configure: (SentryOptions) -> Unit) {
        SentryAndroid.init(context) { options ->
            configure(options)
        }
    }

    fun captureEvent(event: SentryEvent) {
        Sentry.captureEvent(event)
    }

    fun addBreadcrumb(breadcrumb: Breadcrumb) {
        Sentry.addBreadcrumb(breadcrumb)
    }

    fun setUser(user: User?) {
        Sentry.setUser(user)
    }

    fun setTags(tags: Map<String, String>) {
        for ((key, value) in tags) {
            Sentry.setTag(key, value)
        }
    }
}
