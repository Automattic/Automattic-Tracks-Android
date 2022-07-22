package com.automattic.android.tracks.crashlogging.internal

import android.annotation.SuppressLint
import io.sentry.SentryEvent

@SuppressLint("NewApi")
internal fun SentryEvent.appendTags(tags: Map<String, String>) {
    tags.forEach { (key, value) ->
        this.setTag(key, value)
    }
}
