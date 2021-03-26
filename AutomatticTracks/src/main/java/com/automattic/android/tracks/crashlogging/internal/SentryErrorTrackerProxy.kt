package com.automattic.android.tracks.crashlogging.internal

import android.content.Context
import io.sentry.SentryEvent
import io.sentry.SentryOptions
import io.sentry.protocol.User

interface SentryErrorTrackerProxy {

    fun initialize(context: Context, configure: (SentryOptions) -> Unit)

    fun clearBreadcrumbs()

    fun setUser(user: User?)

    fun captureException(exception: Throwable)

    fun captureEvent(event: SentryEvent)

    fun captureMessage(message: String)

    fun applyExtra(key: String, value: String)
}
