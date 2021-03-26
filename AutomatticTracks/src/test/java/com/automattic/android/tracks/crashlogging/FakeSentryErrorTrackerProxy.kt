package com.automattic.android.tracks.crashlogging

import android.content.Context
import com.automattic.android.tracks.crashlogging.internal.SentryErrorTrackerProxy
import io.sentry.SentryEvent
import io.sentry.SentryOptions
import io.sentry.protocol.User

class FakeSentryErrorTrackerProxy : SentryErrorTrackerProxy {

    val extras = mutableMapOf<String, String>()
    var sentryOptions: SentryOptions = SentryOptions()
    var clearBreadcrumbsCounter: Int = 0
    var currentUser: User? = null
    lateinit var capturedException: Throwable
    lateinit var capturedEvent: SentryEvent
    lateinit var capturedMessage: String

    override fun initialize(context: Context, configure: (SentryOptions) -> Unit) {
        configure.invoke(sentryOptions)
    }

    override fun clearBreadcrumbs() {
        clearBreadcrumbsCounter++
    }

    override fun setUser(user: User?) {
        currentUser = user
    }

    override fun captureException(exception: Throwable) {
        capturedException = exception
    }

    override fun captureEvent(event: SentryEvent) {
        capturedEvent = event
    }

    override fun captureMessage(message: String) {
        capturedMessage = message
    }

    override fun applyExtra(key: String, value: String) {
        extras[key] = value
    }
}
