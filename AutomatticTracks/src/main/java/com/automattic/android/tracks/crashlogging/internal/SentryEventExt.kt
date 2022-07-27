package com.automattic.android.tracks.crashlogging.internal

import io.sentry.SentryEvent

internal fun SentryEvent.appendTags(tags: Map<String, String>) {
    for ((key, value) in tags) {
        this.setTag(key, value)
    }
}
