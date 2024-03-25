package com.automattic.android.tracks.crashlogging

data class JsException(
    val type: String,
    val message: String,
    var stackTrace: List<JsExceptionStackTraceElement>,
    val context: Map<String, Any>,
    val tags: Map<String, String>,
    val isHandled: Boolean,
    val handledBy: String
)

data class JsExceptionStackTraceElement(
    val fileName: String?,
    val lineNumber: Int?,
    val colNumber: Int?,
    val function: String
)

interface JsExceptionCallback {
    fun onReportSent(sent: Boolean)
}
