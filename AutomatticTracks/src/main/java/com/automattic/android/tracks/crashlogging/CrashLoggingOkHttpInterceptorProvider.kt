package com.automattic.android.tracks.crashlogging

import io.sentry.android.okhttp.SentryOkHttpInterceptor
import okhttp3.Interceptor

object CrashLoggingOkHttpInterceptorProvider {
    fun createInstance(requestFormatter: RequestFormatter): Interceptor =
        SentryOkHttpInterceptor { span, request, response ->
            requestFormatter.beforeRequestReported(request, response)
            span
        }
}
