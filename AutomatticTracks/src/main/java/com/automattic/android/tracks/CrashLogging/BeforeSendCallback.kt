package com.automattic.android.tracks.CrashLogging

import io.sentry.SentryEvent
import io.sentry.SentryOptions

class BeforeSendCallback(
        private val debug: Boolean,
        private val dataProvider: CrashLoggingDataProvider,
) : SentryOptions.BeforeSendCallback {
    override fun execute(event: SentryEvent, hint: Any?): SentryEvent? {
        return if (debug || dataProvider.userHasOptedOut) {
            null
        } else {
            event
        }
    }
}
