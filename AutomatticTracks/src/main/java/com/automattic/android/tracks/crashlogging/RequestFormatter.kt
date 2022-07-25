package com.automattic.android.tracks.crashlogging

import okhttp3.Request
import okhttp3.Response

/**
 * Formats OkHttp requests before using them as a breadcrumb or performance span
 */
interface RequestFormatter {
    fun beforeRequestReported(request: Request, response: Response?)
}
