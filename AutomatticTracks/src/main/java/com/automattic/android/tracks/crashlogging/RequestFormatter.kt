package com.automattic.android.tracks.crashlogging

import okhttp3.Request

/**
 * Formats OkHttp requests before using them as a breadcrumb or performance span
 *
 * @return formatted url, sent to Sentry
 */
interface RequestFormatter {
    fun formatRequestUrl(request: Request): FormattedUrl
}

typealias FormattedUrl = String
