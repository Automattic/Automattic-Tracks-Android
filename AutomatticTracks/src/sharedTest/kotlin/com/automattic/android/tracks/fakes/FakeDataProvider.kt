package com.automattic.android.tracks.fakes

import com.automattic.android.tracks.BuildConfig
import com.automattic.android.tracks.TracksUser
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import java.util.Locale

class FakeDataProvider(
    var currentUser: TracksUser? = null,
    override val sentryDSN: String = BuildConfig.SENTRY_TEST_PROJECT_DSN,
    val userHasOptedOut: Boolean = false,
    override val buildType: String = "testBuildType",
    val userContext: Map<String, String> = mapOf("user" to "context"),
    val applicationContext: Map<String, String> = mapOf("app" to "context"),
    override val releaseName: String = "testReleaseName",
    override val locale: Locale? = Locale.US,
    override val enableCrashLoggingLogs: Boolean = true,
) : CrashLoggingDataProvider {

    override fun userHasOptedOut(): Boolean = userHasOptedOut

    override fun currentUser(): TracksUser? = currentUser

    override fun applicationContext(): Map<String, String> = applicationContext

    override fun userContext(): Map<String, String> = userContext

    fun updateCurrentUser(newUser: TracksUser) {
        currentUser = newUser
        CrashLogging.setNeedsDataRefresh()
    }
}
