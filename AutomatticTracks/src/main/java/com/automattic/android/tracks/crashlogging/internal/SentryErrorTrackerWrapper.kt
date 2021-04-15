package com.automattic.android.tracks.crashlogging.internal

import android.content.Context
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid

internal class SentryErrorTrackerWrapper {

    fun initialize(context: Context, configure: (SentryOptions) -> Unit) {
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
}
