package com.automattic.android.tracks.networklogger.model

import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Metadata about a log session, included at the top of each log file.
 */
data class LogMetadata(
    val username: String?,
    val appName: String,
    val appVersion: String,
    val platform: String = "android",
    val deviceModel: String,
    val osVersion: String,
    val date: LocalDate
) {
    /**
     * Converts this metadata to a JSON object for storage.
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("username", username)
            put("app_name", appName)
            put("app_version", appVersion)
            put("platform", platform)
            put("device_model", deviceModel)
            put("os_version", osVersion)
            put("date", date.format(DateTimeFormatter.ISO_LOCAL_DATE))
        }
    }
}
