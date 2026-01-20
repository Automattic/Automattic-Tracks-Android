package com.automattic.android.tracks.networklogger

import okhttp3.Headers
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.Buffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Sanitizes request/response data to remove sensitive information before logging.
 */
internal class LogSanitizer(private val config: NetworkLoggerConfig) {

    companion object {
        private const val REDACTED = "[REDACTED]"
    }

    /**
     * Sanitizes HTTP headers by redacting sensitive values.
     * @param headers The original headers
     * @return A map of header names to sanitized values
     */
    fun sanitizeHeaders(headers: Headers): Map<String, String> {
        val sensitiveHeaderNames = config.sensitiveHeaders()
        val result = mutableMapOf<String, String>()

        for (name in headers.names()) {
            val value = if (sensitiveHeaderNames.contains(name.lowercase())) {
                REDACTED
            } else {
                headers[name] ?: ""
            }
            result[name] = value
        }

        return result
    }

    /**
     * Extracts and sanitizes body content from a RequestBody.
     * Returns null if body should not be logged (binary content, too large, etc.)
     */
    fun sanitizeRequestBody(body: RequestBody?): String? {
        if (body == null) return null

        val contentType = body.contentType()
        val contentLength = try {
            body.contentLength()
        } catch (e: Exception) {
            return "[ERROR: Could not read body length]"
        }

        if (!config.shouldLogBody(contentType?.toString(), contentLength)) {
            return "[BODY NOT LOGGED: ${contentType ?: "unknown type"}, $contentLength bytes]"
        }

        return try {
            val buffer = Buffer()
            body.writeTo(buffer)

            val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
            val bodyString = buffer.readString(charset)

            // Truncate if exceeds max size
            if (bodyString.length > config.maxBodySize()) {
                bodyString.substring(0, config.maxBodySize().toInt()) + "\n[TRUNCATED]"
            } else {
                bodyString
            }
        } catch (e: Exception) {
            "[ERROR: Could not read body - ${e.message}]"
        }
    }

    /**
     * Extracts and sanitizes body content from a ResponseBody.
     * Returns null if body should not be logged.
     */
    fun sanitizeResponseBody(body: ResponseBody?): String? {
        if (body == null) return null

        val contentType = body.contentType()
        val contentLength = body.contentLength()

        if (!config.shouldLogBody(contentType?.toString(), contentLength)) {
            return "[BODY NOT LOGGED: ${contentType ?: "unknown type"}, $contentLength bytes]"
        }

        return try {
            val source = body.source()
            source.request(Long.MAX_VALUE) // Buffer the entire body
            val buffer = source.buffer

            val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
            val bodyString = buffer.clone().readString(charset)

            // Truncate if exceeds max size
            if (bodyString.length > config.maxBodySize()) {
                bodyString.substring(0, config.maxBodySize().toInt()) + "\n[TRUNCATED]"
            } else {
                bodyString
            }
        } catch (e: Exception) {
            "[ERROR: Could not read body - ${e.message}]"
        }
    }
}
