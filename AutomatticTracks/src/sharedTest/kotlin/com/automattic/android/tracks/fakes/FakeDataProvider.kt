package com.automattic.android.tracks.fakes

import com.automattic.android.tracks.BuildConfig
import com.automattic.android.tracks.TracksUser
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import java.util.Locale

class FakeDataProvider(
    override val sentryDSN: String = BuildConfig.SENTRY_TEST_PROJECT_DSN,
    override val buildType: String = "testBuildType",
    override val releaseName: String = "testReleaseName",
    override val locale: Locale? = Locale.US,
    override val enableCrashLoggingLogs: Boolean = true,
    var user: TracksUser? = testUser1,
    var userHasOptOut: Boolean = false,
) : CrashLoggingDataProvider {

    override fun userProvider(): TracksUser? {
        return user
    }

    override fun userHasOptOutProvider(): Boolean {
        return userHasOptOut
    }
}
