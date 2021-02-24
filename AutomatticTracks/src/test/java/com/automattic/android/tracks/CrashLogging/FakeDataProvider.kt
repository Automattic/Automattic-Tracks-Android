package com.automattic.android.tracks.CrashLogging

import com.automattic.android.tracks.TracksUser
import java.util.*

open class FakeDataProvider(
        private val userOptedOut: Boolean = false,
        private val currentUser: TracksUser? = null,
) : CrashLoggingDataProvider {
    override fun sentryDSN() = "https://public@sentry.example.com/1"

    override fun getUserHasOptedOut() = userOptedOut

    override fun buildType() = "testBuildType"

    override fun userContext(): MutableMap<String, Any> = mutableMapOf()

    override fun applicationContext(): MutableMap<String, Any> = mutableMapOf()

    override fun releaseName() = "testReleaseName"

    override fun currentUser() = currentUser

    override fun locale(): Locale? = null

}
