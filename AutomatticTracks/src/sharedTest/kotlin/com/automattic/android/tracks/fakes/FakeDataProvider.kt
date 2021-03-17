package com.automattic.android.tracks.fakes

import com.automattic.android.tracks.TracksUser
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import java.util.Locale

open class FakeDataProvider(
    override val currentUser: TracksUser? = null,
    override val sentryDSN: String = "https://public@sentry.example.com/1"
) : CrashLoggingDataProvider {

    override val userHasOptedOut: Boolean = false

    override val buildType = "testBuildType"

    override val userContext = emptyMap<String, String?>()

    override val applicationContext = emptyMap<String, String?>()

    override val releaseName = "testReleaseName"

    override val locale: Locale? = null
}
