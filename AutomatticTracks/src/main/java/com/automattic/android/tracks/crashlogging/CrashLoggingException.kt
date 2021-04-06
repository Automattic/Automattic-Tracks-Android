package com.automattic.android.tracks.crashlogging

import io.sentry.protocol.SentryException

data class CrashLoggingException(
    val module: String,
    val type: String,
    val value: String,
) {
    fun isEqualTo(other: SentryException): Boolean {
        return module == other.module && type == other.type && value == other.value
    }
}
