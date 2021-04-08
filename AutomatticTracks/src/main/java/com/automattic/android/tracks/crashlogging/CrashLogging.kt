package com.automattic.android.tracks.crashlogging

interface CrashLogging {
    fun appendApplicationContext(newApplicationContext: Map<String, String>)
    fun log(throwable: Throwable)
    fun log(throwable: Throwable, data: Map<String, String?>)
    fun log(message: String)
}
