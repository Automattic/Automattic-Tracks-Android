package com.automattic.android.tracks.fakes

import com.automattic.android.tracks.TracksUser
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import java.util.Locale

open class FakeDataProvider(
    override val currentUser: TracksUser? = null,
    override val sentryDSN: String = "https://public@sentry.example.com/1",
    override val userHasOptedOut: Boolean = false,
    override val buildType: String = "testBuildType",
    override val userContext: Map<String, String?> = mapOf("user" to "context"),
    override val applicationContext: Map<String, String?> = mapOf("app" to "context"),
    override val releaseName: String = "testReleaseName",
    override val locale: Locale? = Locale.US,
) : CrashLoggingDataProvider
