package com.automattic.android.tracks.crashlogging

interface CrashLogging {
    fun log(throwable: Throwable)
    fun log(throwable: Throwable, data: Map<String, String?>)
    fun log(message: String)
}
