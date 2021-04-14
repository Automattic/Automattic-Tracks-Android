package com.automattic.android.tracks.crashlogging.internal

import io.sentry.SentryEvent

internal fun SentryEvent.appendTags(tags: Map<String, String>) {
    tags.forEach { (key, value) ->
        this.setTag(key, value)
    }
}
