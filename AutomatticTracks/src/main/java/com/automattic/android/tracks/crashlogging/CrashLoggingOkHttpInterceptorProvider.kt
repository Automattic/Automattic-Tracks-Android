package com.automattic.android.tracks.crashlogging

import io.sentry.android.okhttp.SentryOkHttpInterceptor

object CrashLoggingOkHttpInterceptorProvider {
    fun createInstance(): SentryOkHttpInterceptor = SentryOkHttpInterceptor()
}