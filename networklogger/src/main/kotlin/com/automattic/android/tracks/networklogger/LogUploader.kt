package com.automattic.android.tracks.networklogger

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Handles uploading log files to encrypted logging service with marker file tracking.
 */
internal class LogUploader(
    private val logsDirectory: File,
    private val username: String?,
    private val encryptedLoggingClient: EncryptedLoggingClient,
    private val scope: CoroutineScope
) {

    private val _uploadResults = MutableSharedFlow<UploadResult>(replay = 0)
    val uploadResults: Flow<UploadResult> = _uploadResults

    /**
     * Uploads yesterday's log file if it exists and hasn't been uploaded yet.
     */
    fun uploadYesterdayLogs() {
        scope.launch(Dispatchers.IO) {
            val yesterday = LocalDate.now().minusDays(1)
            val logFile = getLogFile(yesterday)
            val markerFile = getMarkerFile(yesterday)

            if (logFile.exists() && !markerFile.exists()) {
                uploadLogFile(logFile, yesterday)
            }
        }
    }

    /**
     * Uploads a snapshot of today's current logs (for manual upload).
     *
     * @param sourceFile The log file to upload
     * @return UploadResult indicating success or failure
     */
    suspend fun uploadCurrentLogs(sourceFile: File): UploadResult {
        val today = LocalDate.now()

        // Create a snapshot copy
        val snapshotFile = File(logsDirectory, "network_logs_${today}_snapshot.json")
        try {
            sourceFile.copyTo(snapshotFile, overwrite = true)
            return uploadLogFile(snapshotFile, today, isSnapshot = true)
        } finally {
            // Clean up snapshot after upload attempt
            snapshotFile.delete()
        }
    }

    /**
     * Uploads a log file with the given date.
     */
    private suspend fun uploadLogFile(
        file: File,
        date: LocalDate,
        isSnapshot: Boolean = false
    ): UploadResult {
        // Create marker file immediately to prevent duplicate uploads
        val markerFile = if (!isSnapshot) getMarkerFile(date) else null
        markerFile?.createNewFile()

        // Generate upload identifier: username-YYYY-MM-DD
        val uploadId = generateUploadId(date)

        return try {
            // Upload using encrypted logging client
            encryptedLoggingClient.uploadLog(uploadId, file)

            // Delete log file and marker on success (but not for snapshots)
            if (!isSnapshot) {
                file.delete()
                markerFile?.delete()
            }

            UploadResult.Success(uploadId, date)
        } catch (e: Exception) {
            // Remove marker file on failure so it can be retried
            markerFile?.delete()
            UploadResult.Failure(uploadId, date, e.message ?: "Unknown error")
        }
    }

    /**
     * Generates the upload identifier in format: username-YYYY-MM-DD
     */
    private fun generateUploadId(date: LocalDate): String {
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return if (username != null) {
            "$username-$dateString"
        } else {
            "anonymous-$dateString"
        }
    }

    /**
     * Returns the log file for a given date.
     */
    private fun getLogFile(date: LocalDate): File {
        val filename = "network_logs_${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}.json"
        return File(logsDirectory, filename)
    }

    /**
     * Returns the marker file for a given date.
     */
    private fun getMarkerFile(date: LocalDate): File {
        val filename = "network_logs_${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}.uploaded"
        return File(logsDirectory, filename)
    }
}

/**
 * Result of a log upload operation.
 */
sealed class UploadResult {
    data class Success(val uploadId: String, val date: LocalDate) : UploadResult()
    data class Failure(val uploadId: String, val date: LocalDate, val error: String) : UploadResult()
}

/**
 * Interface for encrypted logging client.
 * This will be implemented by a wrapper around the actual EncryptedLogging library.
 */
interface EncryptedLoggingClient {
    /**
     * Uploads a log file with the given identifier.
     *
     * @param uploadId The identifier for this upload (e.g., "username-2026-01-20")
     * @param file The log file to upload
     */
    suspend fun uploadLog(uploadId: String, file: File)
}
