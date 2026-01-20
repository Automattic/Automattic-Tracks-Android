package com.automattic.android.tracks.networklogger

import android.content.Context
import android.os.Build
import com.automattic.android.tracks.networklogger.model.LogMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import java.io.File
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Main API for network logging.
 * Captures HTTP requests/responses and uploads them to encrypted logging service.
 *
 * Usage:
 * ```
 * val networkLogger = NetworkLogger.create(
 *     context = context,
 *     username = "john.doe",
 *     appName = "MyApp",
 *     appVersion = "1.0.0",
 *     encryptedLoggingClient = myEncryptedLoggingClient,
 *     config = NetworkLoggerConfig.default()
 * )
 *
 * // Add interceptor to OkHttp
 * val okHttpClient = OkHttpClient.Builder()
 *     .addInterceptor(networkLogger.getInterceptor())
 *     .build()
 *
 * // Enable logging
 * networkLogger.enable()
 *
 * // Upload yesterday's logs on app start
 * networkLogger.uploadYesterdayLogs()
 * ```
 */
class NetworkLogger private constructor(
    private val buffer: LogBuffer,
    private val fileWriter: LogFileWriter,
    private val uploader: LogUploader,
    private val interceptor: NetworkLoggerInterceptor,
    private val metadata: LogMetadata,
    private val config: NetworkLoggerConfig,
    private val scope: CoroutineScope
) {

    private val isEnabled = AtomicBoolean(false)
    private var flushJob: Job? = null

    /**
     * Enables network logging.
     * Starts periodic flushing of buffer to disk.
     */
    fun enable() {
        if (isEnabled.compareAndSet(false, true)) {
            startPeriodicFlush()
        }
    }

    /**
     * Disables network logging.
     * Stops periodic flushing but retains any buffered data.
     */
    fun disable() {
        if (isEnabled.compareAndSet(true, false)) {
            stopPeriodicFlush()
        }
    }

    /**
     * Returns whether logging is currently enabled.
     */
    fun isEnabled(): Boolean = isEnabled.get()

    /**
     * Returns the OkHttp interceptor to add to your OkHttpClient.
     */
    fun getInterceptor(): Interceptor = interceptor

    /**
     * Uploads yesterday's log file if it exists and hasn't been uploaded yet.
     * This should be called on app start.
     */
    fun uploadYesterdayLogs() {
        uploader.uploadYesterdayLogs()
    }

    /**
     * Manually uploads current logs immediately.
     * Creates a snapshot of today's logs and uploads it.
     *
     * @return UploadResult indicating success or failure
     */
    suspend fun uploadCurrentLogs(): UploadResult {
        // Flush current buffer to file first
        flushBufferToFile()

        // Get today's log file
        val today = LocalDate.now()
        val todayLogFile = fileWriter.getAllLogFiles().find { file ->
            file.name.contains(today.toString())
        }

        return if (todayLogFile != null) {
            uploader.uploadCurrentLogs(todayLogFile)
        } else {
            UploadResult.Failure("", today, "No logs found for today")
        }
    }

    /**
     * Observes upload results as a Flow.
     */
    fun observeUploadResults(): Flow<UploadResult> = uploader.uploadResults

    /**
     * Clears all buffered logs from memory (does not delete files).
     */
    fun clearBuffer() {
        buffer.clear()
    }

    /**
     * Manually flushes the current buffer to disk.
     */
    suspend fun flushBufferToFile() {
        val entries = buffer.drainAll()
        if (entries.isNotEmpty()) {
            fileWriter.writeEntries(entries, metadata)
        }
    }

    /**
     * Starts periodic flushing of buffer to disk.
     */
    private fun startPeriodicFlush() {
        flushJob = scope.launch(Dispatchers.IO) {
            while (isActive && isEnabled.get()) {
                delay(config.flushIntervalMs())
                flushBufferToFile()
            }
        }
    }

    /**
     * Stops periodic flushing.
     */
    private fun stopPeriodicFlush() {
        flushJob?.cancel()
        flushJob = null
    }

    companion object {
        /**
         * Creates a NetworkLogger instance.
         *
         * @param context Android context
         * @param username Username of the current user (null for anonymous)
         * @param appName Name of the application
         * @param appVersion Version of the application
         * @param encryptedLoggingClient Client for uploading encrypted logs
         * @param config Configuration for logging behavior (default: NetworkLoggerConfig.default())
         * @param scope CoroutineScope for async operations (default: CoroutineScope with Dispatchers.IO)
         */
        fun create(
            context: Context,
            username: String?,
            appName: String,
            appVersion: String,
            encryptedLoggingClient: EncryptedLoggingClient,
            config: NetworkLoggerConfig = NetworkLoggerConfig.default(),
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
        ): NetworkLogger {
            val logsDirectory = File(context.cacheDir, "network_logs")

            val buffer = LogBuffer(config.maxBufferSize())
            val fileWriter = LogFileWriter(logsDirectory)
            val sanitizer = LogSanitizer(config)

            val metadata = LogMetadata(
                username = username,
                appName = appName,
                appVersion = appVersion,
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                osVersion = Build.VERSION.RELEASE,
                date = LocalDate.now()
            )

            val isEnabledFunc = { false } // Will be updated by enable()/disable()
            val interceptor = NetworkLoggerInterceptor(buffer, sanitizer, config, isEnabledFunc)

            val uploader = LogUploader(logsDirectory, username, encryptedLoggingClient, scope)

            val logger = NetworkLogger(
                buffer = buffer,
                fileWriter = fileWriter,
                uploader = uploader,
                interceptor = interceptor,
                metadata = metadata,
                config = config,
                scope = scope
            )

            // Update isEnabled function to reference the logger's state
            val updatedInterceptor = NetworkLoggerInterceptor(
                buffer,
                sanitizer,
                config,
                logger::isEnabled
            )

            return NetworkLogger(
                buffer = buffer,
                fileWriter = fileWriter,
                uploader = uploader,
                interceptor = updatedInterceptor,
                metadata = metadata,
                config = config,
                scope = scope
            )
        }
    }
}
