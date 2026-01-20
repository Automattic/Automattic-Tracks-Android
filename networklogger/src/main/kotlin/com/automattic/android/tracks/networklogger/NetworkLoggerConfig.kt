package com.automattic.android.tracks.networklogger

import okhttp3.Request

/**
 * Configuration interface for customizing network logger behavior.
 * Clients can implement this to control what gets logged and how.
 */
interface NetworkLoggerConfig {
    /**
     * Determines whether a request should be logged.
     * @return true if the request should be logged, false otherwise
     */
    fun shouldLogRequest(request: Request): Boolean = true

    /**
     * Determines whether the request body should be logged based on content type and size.
     * @param contentType The Content-Type header value, or null if not present
     * @param size The size of the body in bytes
     * @return true if the body should be logged, false otherwise
     */
    fun shouldLogBody(contentType: String?, size: Long): Boolean {
        // Skip binary content
        if (contentType != null && isBinaryContent(contentType)) {
            return false
        }
        // Check size limit
        return size <= maxBodySize()
    }

    /**
     * Returns the maximum body size to log in bytes.
     * Default is 1MB.
     */
    fun maxBodySize(): Long = 1_048_576 // 1MB

    /**
     * Returns the maximum number of entries to keep in memory buffer.
     * Default is 100 requests.
     */
    fun maxBufferSize(): Int = 100

    /**
     * Returns the interval in milliseconds between flushing buffer to disk.
     * Default is 5 minutes (300,000 ms).
     */
    fun flushIntervalMs(): Long = 300_000 // 5 minutes

    /**
     * Returns the list of header names that should be redacted.
     * Default includes common sensitive headers.
     */
    fun sensitiveHeaders(): Set<String> = setOf(
        "authorization",
        "cookie",
        "set-cookie",
        "x-auth-token",
        "x-api-key",
        "api-key"
    )

    companion object {
        /**
         * Creates a default configuration with standard settings.
         */
        fun default(): NetworkLoggerConfig = object : NetworkLoggerConfig {}

        /**
         * Checks if a content type represents binary content that shouldn't be logged.
         */
        private fun isBinaryContent(contentType: String): Boolean {
            val lowerType = contentType.lowercase()
            return lowerType.startsWith("image/") ||
                lowerType.startsWith("video/") ||
                lowerType.startsWith("audio/") ||
                lowerType.contains("octet-stream")
        }
    }
}
