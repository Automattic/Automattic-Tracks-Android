package com.automattic.android.tracks.crashlogging

interface CrashLogging {

    /**
     * Records a breadcrumb during the app lifecycle but doesn't report an event. This basically
     * adds more context for the next reports created and sent by [sendReport] or an unhandled
     * exception report.
     *
     * @param[message] The message to attach to a breadcrumb
     * @param[category] An optional category
     */
    fun recordEvent(
        message: String,
        category: String? = null
    )

    /**
     * Records a breadcrumb with exception during the app lifecycle but doesn't report an event.
     * This basically adds more context for the next reports created and sent by [sendReport] or an
     * unhandled exception report.
     *
     * @param[exception] The message to attach to a breadcrumb
     * @param[category] An optional category
     */
    fun recordException(
        exception: Throwable,
        category: String? = null
    )

    /**
     * Sends a new event to crash logging service
     *
     * @param[exception] An optional exception to report
     * @param[tags] Tags attached to event
     * @param[message] An optional message attached to event
     */
    fun sendReport(
        exception: Throwable? = null,
        tags: Map<String, String> = emptyMap(),
        message: String? = null
    )
}
