package com.automattic.android.tracks.networklogger

import com.automattic.android.tracks.networklogger.model.LogMetadata
import com.automattic.android.tracks.networklogger.model.NetworkLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Writes network log entries to daily log files using coroutines for async I/O.
 * Each request is tagged with its date to handle midnight boundary correctly.
 */
internal class LogFileWriter(private val logsDirectory: File) {

    init {
        // Ensure logs directory exists
        if (!logsDirectory.exists()) {
            logsDirectory.mkdirs()
        }
    }

    /**
     * Writes a batch of log entries to their respective daily log files.
     * Entries are grouped by date and written to separate files.
     *
     * @param entries The log entries to write
     * @param metadata Metadata about the log session
     */
    suspend fun writeEntries(entries: List<NetworkLogEntry>, metadata: LogMetadata) {
        withContext(Dispatchers.IO) {
            // Group entries by date
            val entriesByDate = entries.groupBy { entry ->
                entry.timestamp.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            }

            // Write each date's entries to its file
            for ((date, dateEntries) in entriesByDate) {
                val file = getLogFile(date)
                appendEntriesToFile(file, dateEntries, metadata.copy(date = date))
            }
        }
    }

    /**
     * Creates a complete log file with metadata and entries for manual upload.
     * This is used for the "Upload Current Logs" feature.
     *
     * @param date The date for the log file
     * @param entries All entries for this date
     * @param metadata Metadata about the log session
     * @return The created file
     */
    suspend fun createCompleteLogFile(
        date: LocalDate,
        entries: List<NetworkLogEntry>,
        metadata: LogMetadata
    ): File {
        return withContext(Dispatchers.IO) {
            val file = getLogFile(date)
            writeCompleteLog(file, entries, metadata.copy(date = date))
            file
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
     * Appends entries to an existing log file, or creates a new one if it doesn't exist.
     */
    private fun appendEntriesToFile(file: File, entries: List<NetworkLogEntry>, metadata: LogMetadata) {
        val existingData = if (file.exists()) {
            try {
                JSONObject(file.readText())
            } catch (e: Exception) {
                // Corrupted file, start fresh
                null
            }
        } else {
            null
        }

        val jsonObject = existingData ?: JSONObject().apply {
            put("metadata", metadata.toJson())
            put("requests", JSONArray())
        }

        val requestsArray = jsonObject.getJSONArray("requests")
        for (entry in entries) {
            requestsArray.put(entry.toJson())
        }

        file.writeText(jsonObject.toString(2)) // Pretty print with indent=2
    }

    /**
     * Writes a complete log file from scratch (used for snapshots).
     */
    private fun writeCompleteLog(file: File, entries: List<NetworkLogEntry>, metadata: LogMetadata) {
        val jsonObject = JSONObject().apply {
            put("metadata", metadata.toJson())
            val requestsArray = JSONArray()
            for (entry in entries) {
                requestsArray.put(entry.toJson())
            }
            put("requests", requestsArray)
        }

        file.writeText(jsonObject.toString(2)) // Pretty print with indent=2
    }

    /**
     * Returns all log files in the logs directory.
     */
    fun getAllLogFiles(): List<File> {
        return logsDirectory.listFiles { file ->
            file.isFile && file.name.startsWith("network_logs_") && file.name.endsWith(".json")
        }?.toList() ?: emptyList()
    }

    /**
     * Returns the log file for yesterday's date, if it exists.
     */
    fun getYesterdayLogFile(): File? {
        val yesterday = LocalDate.now().minusDays(1)
        val file = getLogFile(yesterday)
        return if (file.exists()) file else null
    }
}
