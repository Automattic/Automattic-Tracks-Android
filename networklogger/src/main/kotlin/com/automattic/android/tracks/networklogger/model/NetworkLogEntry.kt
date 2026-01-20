package com.automattic.android.tracks.networklogger.model

import org.json.JSONObject
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Represents a single HTTP request/response captured by the network logger.
 */
data class NetworkLogEntry(
    val timestamp: Instant,
    val method: String,
    val url: String,
    val requestHeaders: Map<String, String>,
    val requestBody: String?,
    val responseCode: Int?,
    val responseHeaders: Map<String, String>?,
    val responseBody: String?,
    val durationMs: Long?,
    val error: String?
) {
    /**
     * Converts this entry to a JSON object for storage.
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("timestamp", timestamp.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            put("method", method)
            put("url", url)
            put("request_headers", JSONObject(requestHeaders))
            put("request_body", requestBody)
            put("response_code", responseCode)
            put("response_headers", responseHeaders?.let { JSONObject(it) })
            put("response_body", responseBody)
            put("duration_ms", durationMs)
            put("error", error)
        }
    }
}
