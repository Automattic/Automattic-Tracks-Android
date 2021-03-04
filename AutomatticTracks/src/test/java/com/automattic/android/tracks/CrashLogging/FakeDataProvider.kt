package com.automattic.android.tracks.CrashLogging

import com.automattic.android.tracks.TracksUser
import java.util.*

open class FakeDataProvider(
        override val currentUser: TracksUser? = null,
) : CrashLoggingDataProvider {

    override val userHasOptedOut: Boolean = false

    override val sentryDSN = "https://public@sentry.example.com/1"

    override val buildType = "testBuildType"

    override val userContext = emptyMap<String, String?>()

    override val applicationContext = emptyMap<String, String?>()

    override val releaseName = "testReleaseName"

    override val locale: Locale? = null

}
