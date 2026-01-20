package com.automattic.android.tracks.networklogger

import com.automattic.android.tracks.networklogger.model.NetworkLogEntry
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.time.Instant

/**
 * OkHttp interceptor that captures HTTP requests and responses for logging.
 * This interceptor is designed to have minimal performance impact.
 */
internal class NetworkLoggerInterceptor(
    private val buffer: LogBuffer,
    private val sanitizer: LogSanitizer,
    private val config: NetworkLoggerConfig,
    private val isEnabled: () -> Boolean
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // Skip if logging is disabled
        if (!isEnabled()) {
            return chain.proceed(chain.request())
        }

        val request = chain.request()

        // Skip if config says not to log this request
        if (!config.shouldLogRequest(request)) {
            return chain.proceed(request)
        }

        val timestamp = Instant.now()
        val startTime = System.currentTimeMillis()

        // Sanitize request data
        val requestHeaders = sanitizer.sanitizeHeaders(request.headers)
        val requestBody = sanitizer.sanitizeRequestBody(request.body)

        // Execute request
        val response: Response
        val error: String?
        try {
            response = chain.proceed(request)
            error = null
        } catch (e: IOException) {
            // Network error occurred
            val entry = NetworkLogEntry(
                timestamp = timestamp,
                method = request.method,
                url = request.url.toString(),
                requestHeaders = requestHeaders,
                requestBody = requestBody,
                responseCode = null,
                responseHeaders = null,
                responseBody = null,
                durationMs = System.currentTimeMillis() - startTime,
                error = "${e.javaClass.simpleName}: ${e.message}"
            )
            buffer.add(entry)
            throw e
        } catch (e: Exception) {
            // Other error
            val entry = NetworkLogEntry(
                timestamp = timestamp,
                method = request.method,
                url = request.url.toString(),
                requestHeaders = requestHeaders,
                requestBody = requestBody,
                responseCode = null,
                responseHeaders = null,
                responseBody = null,
                durationMs = System.currentTimeMillis() - startTime,
                error = "${e.javaClass.simpleName}: ${e.message}"
            )
            buffer.add(entry)
            throw e
        }

        val durationMs = System.currentTimeMillis() - startTime

        // Sanitize response data
        val responseHeaders = sanitizer.sanitizeHeaders(response.headers)
        val responseBody = response.body?.let { body ->
            // Clone the response body so we can read it without consuming it
            val source = body.source()
            source.request(Long.MAX_VALUE)
            val buffer = source.buffer.clone()

            sanitizer.sanitizeResponseBody(body)
        }

        // Create log entry
        val entry = NetworkLogEntry(
            timestamp = timestamp,
            method = request.method,
            url = request.url.toString(),
            requestHeaders = requestHeaders,
            requestBody = requestBody,
            responseCode = response.code,
            responseHeaders = responseHeaders,
            responseBody = responseBody,
            durationMs = durationMs,
            error = if (!response.isSuccessful) "HTTP ${response.code} ${response.message}" else null
        )

        buffer.add(entry)

        return response
    }
}
