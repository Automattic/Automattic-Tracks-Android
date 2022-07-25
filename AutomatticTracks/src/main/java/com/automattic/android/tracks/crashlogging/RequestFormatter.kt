package com.automattic.android.tracks.crashlogging

import okhttp3.Request

/**
 * Formats OkHttp requests before using them as a breadcrumb or performance span
 */
interface RequestFormatter {
    fun formatRequestUrl(request: Request): String
}
